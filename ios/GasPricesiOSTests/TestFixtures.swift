import Compression
import Foundation
@testable import GasPricesiOS

enum TestFixtures {
    static let csvHeader = "e_coicop_code,e_coicop,e_unit_code,e_unit,e_year_code,e_year,e_week_code,e_week,"
        + "e_datefrom_code,e_datefrom,e_dateto_code,e_dateto,e_indic_code,e_indic,value"

    static func sampleData() -> GasPriceData {
        GasPriceData(
            label: "Average fuel prices",
            updated: "10.04.2026 10:00",
            sourceURL: "https://statdata.statistics.sk/public/ui/dc/",
            weeks: [
                WeeklyFuelPrices(year: "2026", week: "W14", dateFrom: "2026-03-30", dateTo: "2026-04-05",
                                 gasoline95: 1.490, gasoline98: 1.590, diesel: 1.430),
                WeeklyFuelPrices(year: "2026", week: "W15", dateFrom: "2026-04-06", dateTo: "2026-04-12",
                                 gasoline95: 1.500, gasoline98: 1.600, diesel: 1.450)
            ]
        )
    }

    static func csvRow(_ code: String, week: String, from: String, to: String, value: String) -> String {
        "\(code),Fuel name,UNIT_EUR_L,Eur / l,2026,2026,\(week),week,x,\(from),x,\(to),U_SP_0001,Fuel,\(value)"
    }

    /// Same shape as the real export: three tracked fuels, one untracked (LPG), unsorted weeks.
    static func sampleCSV() -> String {
        [
            csvHeader,
            csvRow("CP_07202102", week: "W44", from: "2026-10-26", to: "2026-11-01", value: "1.785"),
            csvRow("CP_07202103", week: "W44", from: "2026-10-26", to: "2026-11-01", value: "1.988"),
            csvRow("CP_07202105", week: "W44", from: "2026-10-26", to: "2026-11-01", value: "1.979"),
            csvRow("CP_07202104", week: "W44", from: "2026-10-26", to: "2026-11-01", value: "0.799"),
            csvRow("CP_07202102", week: "W42", from: "2026-10-12", to: "2026-10-18", value: "1.885"),
            csvRow("CP_07202102", week: "W43", from: "2026-10-19", to: "2026-10-25", value: "1.820"),
            csvRow("CP_07202105", week: "W43", from: "2026-10-19", to: "2026-10-25", value: "1.970")
        ].joined(separator: "\n")
    }

    /// Builds a zip like the server's export: Deflate entries, sizes only in the central directory
    /// (local headers carry zeros, as with data descriptors). CRCs are left at zero because the
    /// reader does not verify them.
    static func makeZip(_ files: [(name: String, content: String)]) -> Data {
        var archive = Data()
        var centralDirectory = Data()

        for file in files {
            let raw = Data(file.content.utf8)
            let compressed = deflate(raw)
            let name = Data(file.name.utf8)
            let localOffset = UInt32(archive.count)

            archive.appendUInt32(0x0403_4b50)
            archive.appendUInt16(20)          // version needed
            archive.appendUInt16(0x0808)      // flags: data descriptor + UTF-8
            archive.appendUInt16(8)           // deflate
            archive.appendUInt16(0)           // time
            archive.appendUInt16(0)           // date
            archive.appendUInt32(0)           // crc
            archive.appendUInt32(0)           // compressed size (in descriptor)
            archive.appendUInt32(0)           // uncompressed size (in descriptor)
            archive.appendUInt16(UInt16(name.count))
            archive.appendUInt16(0)
            archive.append(name)
            archive.append(compressed)
            archive.appendUInt32(0x0807_4b50) // data descriptor
            archive.appendUInt32(0)
            archive.appendUInt32(UInt32(compressed.count))
            archive.appendUInt32(UInt32(raw.count))

            centralDirectory.appendUInt32(0x0201_4b50)
            centralDirectory.appendUInt16(20)
            centralDirectory.appendUInt16(20)
            centralDirectory.appendUInt16(0x0808)
            centralDirectory.appendUInt16(8)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt32(0)
            centralDirectory.appendUInt32(UInt32(compressed.count))
            centralDirectory.appendUInt32(UInt32(raw.count))
            centralDirectory.appendUInt16(UInt16(name.count))
            centralDirectory.appendUInt16(0)  // extra
            centralDirectory.appendUInt16(0)  // comment
            centralDirectory.appendUInt16(0)  // disk
            centralDirectory.appendUInt16(0)  // internal attrs
            centralDirectory.appendUInt32(0)  // external attrs
            centralDirectory.appendUInt32(localOffset)
            centralDirectory.append(name)
        }

        let centralOffset = UInt32(archive.count)
        archive.append(centralDirectory)
        archive.appendUInt32(0x0605_4b50)
        archive.appendUInt16(0)
        archive.appendUInt16(0)
        archive.appendUInt16(UInt16(files.count))
        archive.appendUInt16(UInt16(files.count))
        archive.appendUInt32(UInt32(centralDirectory.count))
        archive.appendUInt32(centralOffset)
        archive.appendUInt16(0)
        return archive
    }

    private static func deflate(_ data: Data) -> Data {
        let capacity = data.count + 1024
        var output = Data(count: capacity)
        let written = output.withUnsafeMutableBytes { (out: UnsafeMutableRawBufferPointer) -> Int in
            data.withUnsafeBytes { (input: UnsafeRawBufferPointer) -> Int in
                compression_encode_buffer(
                    out.bindMemory(to: UInt8.self).baseAddress!, capacity,
                    input.bindMemory(to: UInt8.self).baseAddress!, data.count,
                    nil, COMPRESSION_ZLIB
                )
            }
        }
        precondition(written > 0, "deflate failed")
        return output.prefix(written)
    }
}

private extension Data {
    mutating func appendUInt16(_ value: UInt16) {
        append(UInt8(value & 0xff))
        append(UInt8(value >> 8))
    }

    mutating func appendUInt32(_ value: UInt32) {
        appendUInt16(UInt16(value & 0xffff))
        appendUInt16(UInt16(value >> 16))
    }
}
