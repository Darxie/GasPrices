import Compression
import Foundation

enum ZipArchiveError: LocalizedError, Equatable {
    case invalidArchive
    case unsupportedCompression(UInt16)
    case decompressionFailed

    var errorDescription: String? {
        switch self {
        case .invalidArchive:
            return "Downloaded archive is not a valid zip file."
        case .unsupportedCompression(let method):
            return "Downloaded archive uses unsupported compression method \(method)."
        case .decompressionFailed:
            return "Failed to decompress downloaded archive."
        }
    }
}

/// Minimal zip reader for the open data exports: reads the central directory (the exports use
/// data descriptors, so local headers carry no sizes) and inflates stored or Deflate entries
/// with Apple's Compression framework. Zip64 and encryption are not supported.
struct ZipArchiveReader {
    struct Entry: Equatable {
        let name: String
        let compressionMethod: UInt16
        let compressedSize: Int
        let uncompressedSize: Int
        let localHeaderOffset: Int
    }

    private let data: Data
    let entries: [Entry]

    init(data: Data) throws {
        self.data = data
        self.entries = try Self.readCentralDirectory(data)
    }

    func extract(_ entry: Entry) throws -> Data {
        let local = entry.localHeaderOffset
        guard Self.uint32(data, at: local) == 0x0403_4b50 else {
            throw ZipArchiveError.invalidArchive
        }
        let nameLength = Int(Self.uint16(data, at: local + 26))
        let extraLength = Int(Self.uint16(data, at: local + 28))
        let start = local + 30 + nameLength + extraLength
        let end = start + entry.compressedSize
        guard start >= 0, end <= data.count else {
            throw ZipArchiveError.invalidArchive
        }
        let compressed = data.subdata(in: (data.startIndex + start)..<(data.startIndex + end))

        switch entry.compressionMethod {
        case 0:
            return compressed
        case 8:
            return try Self.inflate(compressed, expectedSize: entry.uncompressedSize)
        default:
            throw ZipArchiveError.unsupportedCompression(entry.compressionMethod)
        }
    }

    // MARK: - Parsing

    private static func readCentralDirectory(_ data: Data) throws -> [Entry] {
        // End of central directory record: 22 bytes + optional comment (max 65535 bytes).
        let minEOCD = 22
        guard data.count >= minEOCD else {
            throw ZipArchiveError.invalidArchive
        }
        let searchStart = max(0, data.count - minEOCD - 65_535)
        var eocd: Int?
        var offset = data.count - minEOCD
        while offset >= searchStart {
            if uint32(data, at: offset) == 0x0605_4b50 {
                eocd = offset
                break
            }
            offset -= 1
        }
        guard let eocd else {
            throw ZipArchiveError.invalidArchive
        }

        let entryCount = Int(uint16(data, at: eocd + 10))
        var position = Int(uint32(data, at: eocd + 16))
        var entries: [Entry] = []
        entries.reserveCapacity(entryCount)

        for _ in 0..<entryCount {
            guard position + 46 <= data.count, uint32(data, at: position) == 0x0201_4b50 else {
                throw ZipArchiveError.invalidArchive
            }
            let method = uint16(data, at: position + 10)
            let compressedSize = Int(uint32(data, at: position + 20))
            let uncompressedSize = Int(uint32(data, at: position + 24))
            let nameLength = Int(uint16(data, at: position + 28))
            let extraLength = Int(uint16(data, at: position + 30))
            let commentLength = Int(uint16(data, at: position + 32))
            let localOffset = Int(uint32(data, at: position + 42))
            let nameStart = position + 46
            guard nameStart + nameLength <= data.count else {
                throw ZipArchiveError.invalidArchive
            }
            let nameData = data.subdata(in: (data.startIndex + nameStart)..<(data.startIndex + nameStart + nameLength))
            let name = String(decoding: nameData, as: UTF8.self)

            entries.append(Entry(
                name: name,
                compressionMethod: method,
                compressedSize: compressedSize,
                uncompressedSize: uncompressedSize,
                localHeaderOffset: localOffset
            ))
            position = nameStart + nameLength + extraLength + commentLength
        }
        return entries
    }

    /// COMPRESSION_ZLIB in Apple's Compression framework is raw DEFLATE (RFC 1951),
    /// which is exactly what zip method 8 stores.
    private static func inflate(_ compressed: Data, expectedSize: Int) throws -> Data {
        guard expectedSize > 0 else {
            return Data()
        }
        var output = Data(count: expectedSize)
        let written = output.withUnsafeMutableBytes { (outBuffer: UnsafeMutableRawBufferPointer) -> Int in
            compressed.withUnsafeBytes { (inBuffer: UnsafeRawBufferPointer) -> Int in
                guard let outBase = outBuffer.bindMemory(to: UInt8.self).baseAddress,
                      let inBase = inBuffer.bindMemory(to: UInt8.self).baseAddress else {
                    return 0
                }
                return compression_decode_buffer(
                    outBase, expectedSize,
                    inBase, compressed.count,
                    nil, COMPRESSION_ZLIB
                )
            }
        }
        guard written == expectedSize else {
            throw ZipArchiveError.decompressionFailed
        }
        return output
    }

    private static func uint16(_ data: Data, at offset: Int) -> UInt16 {
        guard offset >= 0, offset + 2 <= data.count else { return 0 }
        let i = data.startIndex + offset
        return UInt16(data[i]) | UInt16(data[i + 1]) << 8
    }

    private static func uint32(_ data: Data, at offset: Int) -> UInt32 {
        guard offset >= 0, offset + 4 <= data.count else { return 0 }
        let i = data.startIndex + offset
        return UInt32(data[i]) | UInt32(data[i + 1]) << 8 | UInt32(data[i + 2]) << 16 | UInt32(data[i + 3]) << 24
    }
}
