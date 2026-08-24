import AppKit
import CoreGraphics

// Renders the Zikr app icon entirely in code: a rounded-squircle emblem
// (matching Apple's macOS icon geometry) with a deep teal→ink gradient
// backdrop and a warm gold crescent + star mark. Output is a single
// 1024x1024 PNG; build.sh derives the rest of the .iconset from it.

let size = 1024.0
let rect = CGRect(x: 0, y: 0, width: size, height: size)

func squirclePath(in rect: CGRect, cornerRatio: CGFloat = 0.225) -> NSBezierPath {
    let radius = rect.width * cornerRatio
    return NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
}

let image = NSImage(size: NSSize(width: size, height: size))
image.lockFocus()

guard let ctx = NSGraphicsContext.current?.cgContext else {
    fatalError("no graphics context")
}

// --- Background squircle with gradient ---
let bgPath = squirclePath(in: rect)
ctx.saveGState()
bgPath.addClip()

let bgColors = [
    CGColor(red: 0.06, green: 0.27, blue: 0.24, alpha: 1),   // #10453D-ish lighter teal
    CGColor(red: 0.02, green: 0.10, blue: 0.10, alpha: 1),   // near-black teal
]
let bgGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: bgColors as CFArray, locations: [0, 1])!
ctx.drawLinearGradient(
    bgGradient,
    start: CGPoint(x: rect.minX, y: rect.maxY),
    end: CGPoint(x: rect.maxX, y: rect.minY),
    options: []
)

// Subtle radial glow behind the emblem for depth.
let glowColors = [
    CGColor(red: 0.20, green: 0.55, blue: 0.47, alpha: 0.55),
    CGColor(red: 0.20, green: 0.55, blue: 0.47, alpha: 0.0),
]
let glowGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: glowColors as CFArray, locations: [0, 1])!
ctx.drawRadialGradient(
    glowGradient,
    startCenter: CGPoint(x: rect.midX, y: rect.midY + size * 0.03),
    startRadius: 0,
    endCenter: CGPoint(x: rect.midX, y: rect.midY + size * 0.03),
    endRadius: size * 0.42,
    options: []
)
ctx.restoreGState()

// --- Crescent + star emblem ---
let goldColors = [
    CGColor(red: 0.98, green: 0.85, blue: 0.58, alpha: 1),
    CGColor(red: 0.80, green: 0.62, blue: 0.27, alpha: 1),
]
let goldGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: goldColors as CFArray, locations: [0, 1])!

func fillWithGoldGradient(_ path: CGPath) {
    ctx.saveGState()
    ctx.addPath(path)
    ctx.clip()
    ctx.drawLinearGradient(
        goldGradient,
        start: CGPoint(x: rect.minX, y: rect.maxY),
        end: CGPoint(x: rect.maxX, y: rect.minY),
        options: []
    )
    ctx.restoreGState()
}

// Crescent = big circle minus an offset smaller circle (even-odd fill).
// Horizontal-only offset gives the classic hilal "C" shape, horns facing right.
let outerRadius = size * 0.225
let outerCenter = CGPoint(x: size * 0.445, y: size * 0.5)
let innerRadius = size * 0.185
let innerCenter = CGPoint(x: size * 0.555, y: size * 0.5)

let crescentPath = CGMutablePath()
crescentPath.addEllipse(in: CGRect(
    x: outerCenter.x - outerRadius, y: outerCenter.y - outerRadius,
    width: outerRadius * 2, height: outerRadius * 2))
crescentPath.addEllipse(in: CGRect(
    x: innerCenter.x - innerRadius, y: innerCenter.y - innerRadius,
    width: innerRadius * 2, height: innerRadius * 2))

ctx.saveGState()
ctx.addPath(crescentPath)
ctx.clip(using: .evenOdd)
ctx.drawLinearGradient(
    goldGradient,
    start: CGPoint(x: rect.minX, y: rect.maxY),
    end: CGPoint(x: rect.maxX, y: rect.minY),
    options: []
)
ctx.restoreGState()

// Small 4-point sparkle star near the crescent's upper tip.
func starPath(center: CGPoint, outerR: CGFloat, innerR: CGFloat) -> CGPath {
    let path = CGMutablePath()
    let points = 4
    for i in 0..<(points * 2) {
        let angle = (CGFloat(i) * .pi / CGFloat(points)) - .pi / 2
        let r = i % 2 == 0 ? outerR : innerR
        let pt = CGPoint(x: center.x + cos(angle) * r, y: center.y + sin(angle) * r)
        if i == 0 { path.move(to: pt) } else { path.addLine(to: pt) }
    }
    path.closeSubpath()
    return path
}

let starCenter = CGPoint(x: size * 0.585, y: size * 0.435)
let star = starPath(center: starCenter, outerR: size * 0.044, innerR: size * 0.015)
fillWithGoldGradient(star)

image.unlockFocus()

// --- Write PNG ---
guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("failed to encode PNG")
}

let outputPath = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "Resources/icon_1024.png"
try! png.write(to: URL(fileURLWithPath: outputPath))
print("Wrote \(outputPath)")
