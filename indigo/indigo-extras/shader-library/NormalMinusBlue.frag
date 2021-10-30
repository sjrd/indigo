#version 300 es

precision mediump float;

uniform sampler2D SRC_CHANNEL;

vec4 CHANNEL_0;
vec4 COLOR;
vec2 UV;
vec2 TEXTURE_SIZE;
vec2 SIZE;
vec2 CHANNEL_0_POSITION;
vec2 CHANNEL_0_SIZE;

//<indigo-fragment>
layout (std140) uniform IndigoBitmapData {
  highp float FILLTYPE;
};

void fragment(){

  // 0 = normal; 1 = stretch; 2 = tile
  int fillType = int(round(FILLTYPE));
  vec4 textureColor;

  switch(fillType) {
    case 0:
      textureColor = CHANNEL_0;
      break;

    case 1:
      vec2 stretchedUVs = CHANNEL_0_POSITION + UV * CHANNEL_0_SIZE;
      textureColor = texture(SRC_CHANNEL, stretchedUVs);
      break;

    case 2:
      vec2 tiledUVs = CHANNEL_0_POSITION + (fract(UV * (SIZE / TEXTURE_SIZE)) * CHANNEL_0_SIZE);
      textureColor = texture(SRC_CHANNEL, tiledUVs);
      break;

    default:
      textureColor = CHANNEL_0;
      break;
  }

  vec3 redGreen = vec3(textureColor.rg, 0.0);
  float alpha;
  
  if(abs(redGreen.r - 0.5) < 0.01 && abs(redGreen.g - 0.5) < 0.01) {
    alpha = 0.0;
  } else {
    alpha = max(redGreen.r, redGreen.g);
  }

  COLOR = vec4(textureColor.rg * alpha, 0.0, alpha);
}
//</indigo-fragment>