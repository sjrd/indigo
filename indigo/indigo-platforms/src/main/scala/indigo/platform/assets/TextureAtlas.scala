package indigo.platform.assets

import indigo.shared.PowerOfTwo
import indigo.shared.datatypes.Point
import indigo.shared.IndigoLogger
import indigo.shared.EqualTo

import org.scalajs.dom
import org.scalajs.dom.{html, raw}

import scala.annotation.tailrec
import indigo.shared.assets.AssetName

object TextureAtlas {

  import TextureAtlasFunctions._

  val IdPrefix: String = "atlas_"

  val MaxTextureSize: PowerOfTwo = PowerOfTwo._4096

  val supportedSizes: Set[PowerOfTwo] = PowerOfTwo.all

  def createWithMaxSize(
      max: PowerOfTwo,
      imageRefs: List[ImageRef],
      lookupByName: String => Option[LoadedImageAsset],
      createAtlasFunc: (TextureMap, String => Option[LoadedImageAsset]) => Atlas
  ): TextureAtlas =
    (inflateAndSortByPowerOfTwo andThen groupTexturesIntoAtlasBuckets(max) andThen convertToAtlas(createAtlasFunc)(lookupByName))(
      imageRefs
    )

  def create(imageRefs: List[ImageRef], lookupByName: String => Option[LoadedImageAsset], createAtlasFunc: (TextureMap, String => Option[LoadedImageAsset]) => Atlas): TextureAtlas = {
    IndigoLogger.info(s"Creating atlases. Max size: ${MaxTextureSize.value.toString()}x${MaxTextureSize.value.toString()}")
    val textureAtlas = (inflateAndSortByPowerOfTwo andThen groupTexturesIntoAtlasBuckets(MaxTextureSize) andThen convertToAtlas(
      createAtlasFunc
    )(lookupByName))(imageRefs)

    IndigoLogger.info(textureAtlas.report)

    textureAtlas
  }

  val identity: TextureAtlas = TextureAtlas(Map.empty[AtlasId, Atlas], Map.empty[String, AtlasIndex])

}

// Output
final case class TextureAtlas(atlases: Map[AtlasId, Atlas], legend: Map[String, AtlasIndex]) {
  def +(other: TextureAtlas): TextureAtlas =
    TextureAtlas(
      this.atlases ++ other.atlases,
      this.legend ++ other.legend
    )

  def lookUpByName(name: String): Option[AtlasLookupResult] =
    legend.get(name).flatMap { i =>
      atlases.get(i.id).map { a =>
        new AtlasLookupResult(name, i.id, a, i.offset)
      }
    }

  def report: String = {
    val atlasRecordToString: Map[String, AtlasIndex] => ((AtlasId, Atlas)) => String = leg =>
      at => {
        val relevant = leg.filter(k => implicitly[EqualTo[AtlasId]].equal(k._2.id, at._1))

        s"Atlas [${at._1.id}] [${at._2.size.value.toString()}] contains images: ${relevant.toList.map(_._1).mkString(", ")}"
      }

    s"""Atlas details:
    |Number of atlases: ${atlases.keys.toList.length.toString()}
    |Atlases: [
    |  ${atlases.map(atlasRecordToString(legend)).mkString("\n  ")}
    |]
  """.stripMargin
  }

}

final class AtlasId(val id: String) extends AnyVal
object AtlasId {

  implicit val equalTo: EqualTo[AtlasId] = {
    val eqS = implicitly[EqualTo[String]]

    EqualTo.create { (a, b) =>
      eqS.equal(a.id, b.id)
    }
  }

}

final class AtlasIndex(val id: AtlasId, val offset: Point)
object AtlasIndex {

  implicit val equalTo: EqualTo[AtlasIndex] = {
    val eqId = implicitly[EqualTo[AtlasId]]
    val eqPt = implicitly[EqualTo[Point]]

    EqualTo.create { (a, b) =>
      eqId.equal(a.id, b.id) && eqPt.equal(a.offset, b.offset)
    }
  }

}

final class Atlas(val size: PowerOfTwo, val imageData: Option[raw.ImageData]) // Yuk. Only optional so that testing is bearable.
object Atlas {

  implicit val equalTo: EqualTo[Atlas] = {
    val eqP2 = implicitly[EqualTo[PowerOfTwo]]
    val eqB  = implicitly[EqualTo[Boolean]]

    EqualTo.create { (a, b) =>
      eqP2.equal(a.size, b.size) && eqB.equal(a.imageData.isDefined, b.imageData.isDefined)
    }
  }

}

final class AtlasLookupResult(val name: String, val atlasId: AtlasId, val atlas: Atlas, val offset: Point)
object AtlasLookupResult {

  implicit val equalTo: EqualTo[AtlasLookupResult] = {
    val eqS  = implicitly[EqualTo[String]]
    val eqId = implicitly[EqualTo[AtlasId]]
    val eqAt = implicitly[EqualTo[Atlas]]
    val eqPt = implicitly[EqualTo[Point]]

    EqualTo.create { (a, b) =>
      eqS.equal(a.name, b.name) &&
      eqId.equal(a.atlasId, b.atlasId) &&
      eqAt.equal(a.atlas, b.atlas) &&
      eqPt.equal(a.offset, b.offset)
    }
  }

}

object TextureAtlasFunctions {

  /**
    * Type fails all over the place, no guarantee that this list is in the right order...
    * so instead of just going through the set until we find a bigger value, we have to filter and fold all
    */
  def pickPowerOfTwoSizeFor(supportedSizes: Set[PowerOfTwo], width: Int, height: Int): PowerOfTwo =
    supportedSizes
      .filter(s => s.value >= width && s.value >= height)
      .foldLeft(PowerOfTwo.Max)(PowerOfTwo.min)

  def isTooBig(max: PowerOfTwo, width: Int, height: Int): Boolean = if (width > max.value || height > max.value) true else false

  val inflateAndSortByPowerOfTwo: List[ImageRef] => List[TextureDetails] = images =>
    images
      .map(i => TextureDetails(i, TextureAtlasFunctions.pickPowerOfTwoSizeFor(TextureAtlas.supportedSizes, i.width, i.height), i.tag))
      .sortBy(_.size.value)
      .reverse

  def groupTexturesIntoAtlasBuckets(max: PowerOfTwo): List[TextureDetails] => List[List[TextureDetails]] =
    list => {
      val runningTotal: List[TextureDetails] => Int = _.map(_.size.value).sum

      @tailrec
      def createBuckets(
          remaining: List[TextureDetails],
          current: List[TextureDetails],
          rejected: List[TextureDetails],
          acc: List[List[TextureDetails]],
          maximum: PowerOfTwo
      ): List[List[TextureDetails]] =
        (remaining, rejected) match {
          case (Nil, Nil) =>
            current :: acc

          case (Nil, x :: xs) =>
            createBuckets(x :: xs, Nil, Nil, current :: acc, maximum)

          case (x :: xs, _) if x.size >= maximum =>
            createBuckets(xs, current, rejected, List(x) :: acc, maximum)

          case (x :: xs, _) if runningTotal(current) + x.size.value > maximum.value * 2 =>
            createBuckets(xs, current, x :: rejected, acc, maximum)

          case (x :: xs, _) =>
            createBuckets(xs, x :: current, rejected, acc, maximum)

        }

      // @tailrec
      // def splitByTags(remaining: List[TextureDetails]): List[List[TextureDetails]] =
      //   remaining.sortBy(_.tag.getOrElse("")) match {

      //   }

      def sortAndGroupByTag: List[TextureDetails] => List[(String, List[TextureDetails])] =
        _.groupBy(_.tag.getOrElse("")).toList.sortBy(_._1)

      // val x: List[List[TextureDetails]] =
      sortAndGroupByTag(list).flatMap {
        case (_, tds) =>
          createBuckets(tds, Nil, Nil, Nil, max)
      }

      // x

      // createBuckets(list, Nil, Nil, Nil, max)
    }

  // @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def createCanvas(width: Int, height: Int): html.Canvas = {
    val canvas: html.Canvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    // Handy if you want to draw the atlas to the page...
//    dom.document.body.appendChild(canvas)
    canvas.width = width
    canvas.height = height

    canvas
  }

  // @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  val createAtlasData: (TextureMap, String => Option[LoadedImageAsset]) => Atlas = (textureMap, lookupByName) => {
    val canvas: html.Canvas = createCanvas(textureMap.size.value, textureMap.size.value)
    val ctx                 = canvas.getContext("2d")

    textureMap.textureCoords.foreach { tex =>
      lookupByName(tex.imageRef.name.value).foreach { img =>
        ctx.drawImage(img.data, tex.coords.x, tex.coords.y, tex.imageRef.width, tex.imageRef.height)
      }

    }

    val imageData: raw.ImageData =
      ctx.getImageData(0, 0, textureMap.size.value, textureMap.size.value).asInstanceOf[raw.ImageData]

    new Atlas(textureMap.size, Option(imageData))
  }

  val convertTextureDetailsToTree: TextureDetails => AtlasQuadTree = textureDetails => AtlasQuadNode(textureDetails.size, AtlasTexture(textureDetails.imageRef))

  val convertToTextureAtlas: ((TextureMap, String => Option[LoadedImageAsset]) => Atlas) => (
      String => Option[LoadedImageAsset]
  ) => (AtlasId, List[TextureDetails]) => TextureAtlas = createAtlasFunc =>
    lookupByName =>
      (atlasId, list) =>
        list.map(convertTextureDetailsToTree).foldLeft(AtlasQuadTree.identity)(_ + _) match {
          case AtlasQuadEmpty(_) => TextureAtlas.identity
          case n: AtlasQuadNode =>
            val textureMap = n.toTextureMap

            val legend: Map[String, AtlasIndex] =
              textureMap.textureCoords.foldLeft(Map.empty[String, AtlasIndex])((m, t) => m ++ Map(t.imageRef.name.value -> new AtlasIndex(atlasId, t.coords)))

            val atlas = createAtlasFunc(textureMap, lookupByName)

            TextureAtlas(
              atlases = Map(
                atlasId -> atlas
              ),
              legend = legend
            )
        }

  val combineTextureAtlases: List[TextureAtlas] => TextureAtlas = list => list.foldLeft(TextureAtlas.identity)(_ + _)

  val convertToAtlas: ((TextureMap, String => Option[LoadedImageAsset]) => Atlas) => (
      String => Option[LoadedImageAsset]
  ) => List[List[TextureDetails]] => TextureAtlas = createAtlasFunc =>
    lookupByName =>
      list =>
        combineTextureAtlases(
          list.zipWithIndex
            .map(p => convertToTextureAtlas(createAtlasFunc)(lookupByName)(new AtlasId(TextureAtlas.IdPrefix + p._2.toString), p._1))
        )

  def mergeTrees(a: AtlasQuadTree, b: AtlasQuadTree, max: PowerOfTwo): Option[AtlasQuadTree] =
    (a, b) match {
      case (AtlasQuadEmpty(_), AtlasQuadEmpty(_)) =>
        Some(a)

      case (AtlasQuadNode(_, _), AtlasQuadEmpty(_)) =>
        Some(a)

      case (AtlasQuadEmpty(_), AtlasQuadNode(_, _)) =>
        Some(b)

      case (AtlasQuadNode(_, _), AtlasQuadNode(sizeB, _)) if a.canAccommodate(sizeB) =>
        mergeTreeBIntoA(a, b)

      case (AtlasQuadNode(sizeA, _), AtlasQuadNode(_, _)) if b.canAccommodate(sizeA) =>
        mergeTreeBIntoA(b, a)

      case (AtlasQuadNode(sizeA, _), AtlasQuadNode(sizeB, _)) if sizeA >= sizeB && sizeA.doubled <= max =>
        mergeTreeBIntoA(createEmptyTree(calculateSizeNeededToHouseAB(sizeA, sizeB)), a).flatMap { c =>
          mergeTreeBIntoA(c, b)
        }

      case (AtlasQuadNode(sizeA, _), AtlasQuadNode(sizeB, _)) if sizeB >= sizeA && sizeB.doubled <= max =>
        mergeTreeBIntoA(createEmptyTree(calculateSizeNeededToHouseAB(sizeA, sizeB)), b).flatMap { c =>
          mergeTreeBIntoA(c, a)
        }

      case _ =>
        IndigoLogger.info("Could not merge trees")
        None
    }

  def mergeTreeBIntoA(a: AtlasQuadTree, b: AtlasQuadTree): Option[AtlasQuadTree] =
    if (!a.canAccommodate(b.size) && !b.canAccommodate(a.size)) None
    else
      Option {
        if (a.canAccommodate(b.size)) a.insert(b) else b.insert(a)
      }

  def calculateSizeNeededToHouseAB(sizeA: PowerOfTwo, sizeB: PowerOfTwo): PowerOfTwo =
    if (sizeA >= sizeB) sizeA.doubled else sizeB.doubled

  def createEmptyTree(size: PowerOfTwo): AtlasQuadNode = AtlasQuadNode(size, AtlasQuadDivision.empty(size.halved))

}

// Input
final case class ImageRef(name: AssetName, width: Int, height: Int, tag: Option[String])

final case class TextureDetails(imageRef: ImageRef, size: PowerOfTwo, tag: Option[String])

final case class TextureMap(size: PowerOfTwo, textureCoords: List[TextureAndCoords])
final case class TextureAndCoords(imageRef: ImageRef, coords: Point)

sealed trait AtlasQuadTree {
  val size: PowerOfTwo
  def canAccommodate(requiredSize: PowerOfTwo): Boolean
  def insert(tree: AtlasQuadTree): AtlasQuadTree

  def +(other: AtlasQuadTree): AtlasQuadTree = AtlasQuadTree.append(this, other)

  def toTextureCoordsList(offset: Point): List[TextureAndCoords]
}

object AtlasQuadTree {

  def identity: AtlasQuadTree = AtlasQuadEmpty(PowerOfTwo._2)

  def append(first: AtlasQuadTree, second: AtlasQuadTree): AtlasQuadTree =
    TextureAtlasFunctions.mergeTrees(first, second, PowerOfTwo.Max).getOrElse(first)

}

final case class AtlasQuadNode(size: PowerOfTwo, atlas: AtlasSum) extends AtlasQuadTree {
  def canAccommodate(requiredSize: PowerOfTwo): Boolean =
    if (size < requiredSize) false
    else atlas.canAccommodate(requiredSize)

  // @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def insert(tree: AtlasQuadTree): AtlasQuadTree =
    this.copy(atlas = atlas match {
      case AtlasTexture(_) => this.atlas

      case d @ AtlasQuadDivision(AtlasQuadEmpty(s), _, _, _) if s === tree.size =>
        d.copy(q1 = tree)
      case d @ AtlasQuadDivision(_, AtlasQuadEmpty(s), _, _) if s === tree.size =>
        d.copy(q2 = tree)
      case d @ AtlasQuadDivision(_, _, AtlasQuadEmpty(s), _) if s === tree.size =>
        d.copy(q3 = tree)
      case d @ AtlasQuadDivision(_, _, _, AtlasQuadEmpty(s)) if s === tree.size =>
        d.copy(q4 = tree)

      case d @ AtlasQuadDivision(AtlasQuadEmpty(s), _, _, _) if s > tree.size =>
        d.copy(q1 = TextureAtlasFunctions.createEmptyTree(s).insert(tree))
      case d @ AtlasQuadDivision(_, AtlasQuadEmpty(s), _, _) if s > tree.size =>
        d.copy(q2 = TextureAtlasFunctions.createEmptyTree(s).insert(tree))
      case d @ AtlasQuadDivision(_, _, AtlasQuadEmpty(s), _) if s > tree.size =>
        d.copy(q3 = TextureAtlasFunctions.createEmptyTree(s).insert(tree))
      case d @ AtlasQuadDivision(_, _, _, AtlasQuadEmpty(s)) if s > tree.size =>
        d.copy(q4 = TextureAtlasFunctions.createEmptyTree(s).insert(tree))

      case d @ AtlasQuadDivision(AtlasQuadNode(_, _), _, _, _) if d.q1.canAccommodate(tree.size) =>
        d.copy(q1 = d.q1.insert(tree))
      case d @ AtlasQuadDivision(_, AtlasQuadNode(_, _), _, _) if d.q2.canAccommodate(tree.size) =>
        d.copy(q2 = d.q2.insert(tree))
      case d @ AtlasQuadDivision(_, _, AtlasQuadNode(_, _), _) if d.q3.canAccommodate(tree.size) =>
        d.copy(q3 = d.q3.insert(tree))
      case d @ AtlasQuadDivision(_, _, _, AtlasQuadNode(_, _)) if d.q4.canAccommodate(tree.size) =>
        d.copy(q4 = d.q4.insert(tree))

      case _ =>
        IndigoLogger.info("Unexpected failure to insert tree")
        this.atlas
    })

  def toTextureCoordsList(offset: Point): List[TextureAndCoords] =
    atlas match {
      case AtlasTexture(imageRef) =>
        List(TextureAndCoords(imageRef, offset))

      case AtlasQuadDivision(q1, q2, q3, q4) =>
        q1.toTextureCoordsList(offset) ++
          q2.toTextureCoordsList(offset + size.halved.toPoint.withY(0)) ++
          q3.toTextureCoordsList(offset + size.halved.toPoint.withX(0)) ++
          q4.toTextureCoordsList(offset + size.halved.toPoint)

    }

  def toTextureMap: TextureMap =
    TextureMap(size, toTextureCoordsList(Point.zero))
}

final case class AtlasQuadEmpty(size: PowerOfTwo) extends AtlasQuadTree {
  def canAccommodate(requiredSize: PowerOfTwo): Boolean = size >= requiredSize
  def insert(tree: AtlasQuadTree): AtlasQuadTree        = this

  def toTextureCoordsList(offset: Point): List[TextureAndCoords] = Nil
}

sealed trait AtlasSum {
  def canAccommodate(requiredSize: PowerOfTwo): Boolean
}

final case class AtlasTexture(imageRef: ImageRef) extends AtlasSum {
  def canAccommodate(requiredSize: PowerOfTwo): Boolean = false
}

final case class AtlasQuadDivision(q1: AtlasQuadTree, q2: AtlasQuadTree, q3: AtlasQuadTree, q4: AtlasQuadTree) extends AtlasSum {
  def canAccommodate(requiredSize: PowerOfTwo): Boolean =
    q1.canAccommodate(requiredSize) || q2.canAccommodate(requiredSize) || q3.canAccommodate(requiredSize) || q4.canAccommodate(
      requiredSize
    )
}

object AtlasQuadDivision {
  def empty(size: PowerOfTwo): AtlasQuadDivision =
    AtlasQuadDivision(AtlasQuadEmpty(size), AtlasQuadEmpty(size), AtlasQuadEmpty(size), AtlasQuadEmpty(size))
}
