#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 center;
uniform float intensity;
uniform float strength;
uniform float blurStart;
uniform float blurFull;
uniform float warpPulse;
uniform float warpStrength;
uniform float warpStart;
uniform float warpFull;
uniform float chromaticStrength;
uniform int samples;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 direction = texCoord - center;
    float distanceFromCenter = length(direction);
    vec2 normalizedDirection = vec2(0.0);
    if (distanceFromCenter > 0.00001) {
        normalizedDirection = direction / distanceFromCenter;
    }

    vec2 rectangularDistance = abs(direction) * 2.0;
    float edgePosition = max(rectangularDistance.x, rectangularDistance.y);
    float edgeMask = smoothstep(blurStart, blurFull, edgePosition);
    float warpMask = smoothstep(warpStart, warpFull, edgePosition);
    float warpAmount = clamp(warpStrength * warpPulse * warpMask, 0.0, 0.95);
    vec2 warpedUv = clamp(center + direction * (1.0 - warpAmount), vec2(0.001), vec2(0.999));
    vec2 offset = normalizedDirection * strength * intensity * edgeMask;

    vec4 color = texture(DiffuseSampler, warpedUv);
    float totalWeight = 1.0;
    for (int sampleIndex = 1; sampleIndex <= samples; sampleIndex++) {
        float progress = float(sampleIndex) / float(samples);
        float weight = pow(1.0 - progress, 2.0);
        vec2 sampleUv = clamp(warpedUv - offset * progress, vec2(0.001), vec2(0.999));
        color += texture(DiffuseSampler, sampleUv) * weight;
        totalWeight += weight;
    }

    color /= totalWeight;
    float chromaticMask = warpPulse * warpMask;
    vec2 chromaticOffset = normalizedDirection * chromaticStrength * chromaticMask;
    color.r = texture(DiffuseSampler, clamp(warpedUv + chromaticOffset, vec2(0.001), vec2(0.999))).r;
    color.b = texture(DiffuseSampler, clamp(warpedUv - chromaticOffset, vec2(0.001), vec2(0.999))).b;
    fragColor = color;
}
