import AppKit
import CoreGraphics

// Renders the 1200x630 Open Graph / Twitter card banner for the GitHub
// Pages site: same teal/gold crescent mark as AppIcon, plus wordmark and
// tagline, all drawn in code (no external image assets).

let width = 1200.0
let height = 630.0
let rect = CGRect(x: 0, y: 0, width: width, height: height)

let image = NSImage(size: NSSize(width: width, height: height))
image.lockFocus()
guard let ctx = NSGraphicsContext.current?.cgContext else { fatalError("no graphics context") }

// --- Background gradient ---
let bgColors = [
    CGColor(red: 0.06, green: 0.27, blue: 0.24, alpha: 1),
    CGColor(red: 0.02, green: 0.10, blue: 0.10, alpha: 1),
]
let bgGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: bgColors as CFArray, locations: [0, 1])!
ctx.drawLinearGradient(bgGradient, start: CGPoint(x: rect.minX, y: rect.maxY), end: CGPoint(x: rect.maxX, y: rect.minY), options: [])

let glowColors = [
    CGColor(red: 0.20, green: 0.55, blue: 0.47, alpha: 0.45),
    CGColor(red: 0.20, green: 0.55, blue: 0.47, alpha: 0.0),
]
let glowGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: glowColors as CFArray, locations: [0, 1])!
ctx.drawRadialGradient(
    glowGradient,
    startCenter: CGPoint(x: rect.width * 0.24, y: rect.midY),
    startRadius: 0,
    endCenter: CGPoint(x: rect.width * 0.24, y: rect.midY),
    endRadius: height * 0.55,
    options: []
)

// --- Gold gradient for emblem + text ---
let goldColors = [
    CGColor(red: 0.98, green: 0.85, blue: 0.58, alpha: 1),
    CGColor(red: 0.80, green: 0.62, blue: 0.27, alpha: 1),
]
let goldGradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: goldColors as CFArray, locations: [0, 1])!

func fillWithGold(_ path: CGPath) {
    ctx.saveGState()
    ctx.addPath(path)
    ctx.clip()
    ctx.drawLinearGradient(goldGradient, start: CGPoint(x: rect.minX, y: rect.maxY), end: CGPoint(x: rect.midX, y: rect.minY), options: [])
    ctx.restoreGState()
}

// --- Crescent emblem (left side) ---
let emblemCenter = CGPoint(x: height * 0.42, y: height * 0.5)
let outerRadius = height * 0.26
let outerCenter = CGPoint(x: emblemCenter.x - height * 0.02, y: emblemCenter.y)
let innerRadius = height * 0.215
let innerCenter = CGPoint(x: emblemCenter.x + height * 0.06, y: emblemCenter.y)

let crescentPath = CGMutablePath()
crescentPath.addEllipse(in: CGRect(x: outerCenter.x - outerRadius, y: outerCenter.y - outerRadius, width: outerRadius * 2, height: outerRadius * 2))
crescentPath.addEllipse(in: CGRect(x: innerCenter.x - innerRadius, y: innerCenter.y - innerRadius, width: innerRadius * 2, height: innerRadius * 2))

ctx.saveGState()
ctx.addPath(crescentPath)
ctx.clip(using: .evenOdd)
ctx.drawLinearGradient(goldGradient, start: CGPoint(x: rect.minX, y: rect.maxY), end: CGPoint(x: rect.midX, y: rect.minY), options: [])
ctx.restoreGState()

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

let starCenter = CGPoint(x: emblemCenter.x + height * 0.09, y: emblemCenter.y - height * 0.10)
fillWithGold(starPath(center: starCenter, outerR: height * 0.055, innerR: height * 0.018))

// --- Wordmark + tagline (right of emblem) ---
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)

let textX = height * 0.42 + height * 0.30

let titleAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 96, weight: .semibold),
    .foregroundColor: NSColor(calibratedWhite: 0.97, alpha: 1),
]
let title = NSAttributedString(string: "Zikr", attributes: titleAttrs)
title.draw(at: CGPoint(x: textX, y: height * 0.52))

let taglineAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 34, weight: .regular),
    .foregroundColor: NSColor(calibratedWhite: 0.80, alpha: 1),
]
let tagline = NSAttributedString(string: "A quiet dhikr reminder for your Mac", attributes: taglineAttrs)
tagline.draw(at: CGPoint(x: textX, y: height * 0.40))

let subAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 24, weight: .medium),
    .foregroundColor: NSColor(red: 0.80, green: 0.62, blue: 0.27, alpha: 1),
]
let sub = NSAttributedString(string: "MENU BAR  ·  FREE  ·  OPEN SOURCE", attributes: subAttrs)
sub.draw(at: CGPoint(x: textX, y: height * 0.30))

NSGraphicsContext.restoreGraphicsState()

image.unlockFocus()

guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("failed to encode PNG")
}

let outputPath = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "docs/og-banner.png"
try! png.write(to: URL(fileURLWithPath: outputPath))
print("Wrote \(outputPath)")
