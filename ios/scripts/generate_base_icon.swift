import AppKit
import Foundation

if CommandLine.arguments.count < 2 {
    fatalError("Missing output path argument")
}

let outputPath = CommandLine.arguments[1]
let size = NSSize(width: 1024, height: 1024)
let image = NSImage(size: size)

image.lockFocus()
let rect = NSRect(origin: .zero, size: size)

let bgGradient = NSGradient(colors: [
    NSColor(calibratedRed: 0.07, green: 0.12, blue: 0.20, alpha: 1.0),
    NSColor(calibratedRed: 0.13, green: 0.59, blue: 0.95, alpha: 1.0)
])!
bgGradient.draw(in: rect, angle: -40)

let glow = NSBezierPath(ovalIn: NSRect(x: 140, y: 130, width: 744, height: 744))
NSColor(calibratedRed: 1.0, green: 0.76, blue: 0.03, alpha: 0.16).setFill()
glow.fill()

let paragraph = NSMutableParagraphStyle()
paragraph.alignment = .center

let topAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 168, weight: .bold),
    .foregroundColor: NSColor.white,
    .paragraphStyle: paragraph
]
NSString(string: "95   98   D").draw(in: NSRect(x: 0, y: 448, width: 1024, height: 210), withAttributes: topAttrs)

let midAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 84, weight: .semibold),
    .foregroundColor: NSColor(calibratedWhite: 1.0, alpha: 0.92),
    .paragraphStyle: paragraph
]
NSString(string: "GAS PRICES").draw(in: NSRect(x: 0, y: 320, width: 1024, height: 120), withAttributes: midAttrs)

let bottomAttrs: [NSAttributedString.Key: Any] = [
    .font: NSFont.systemFont(ofSize: 60, weight: .medium),
    .foregroundColor: NSColor(calibratedWhite: 1.0, alpha: 0.85),
    .paragraphStyle: paragraph
]
NSString(string: "SLOVAKIA").draw(in: NSRect(x: 0, y: 248, width: 1024, height: 90), withAttributes: bottomAttrs)

let linePath = NSBezierPath()
linePath.move(to: NSPoint(x: 210, y: 220))
linePath.line(to: NSPoint(x: 390, y: 278))
linePath.line(to: NSPoint(x: 515, y: 255))
linePath.line(to: NSPoint(x: 690, y: 324))
linePath.line(to: NSPoint(x: 814, y: 296))
linePath.lineWidth = 18
NSColor(calibratedRed: 1.0, green: 0.95, blue: 0.80, alpha: 0.9).setStroke()
linePath.stroke()

image.unlockFocus()

if let tiff = image.tiffRepresentation,
   let rep = NSBitmapImageRep(data: tiff),
   let png = rep.representation(using: .png, properties: [:]) {
    try png.write(to: URL(fileURLWithPath: outputPath))
}