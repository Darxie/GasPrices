import DGCharts
import UIKit

final class GasPriceMarkerView: MarkerView {
    private let markerLabel = UILabel()
    private var weekLabels: [String]

    init(weekLabels: [String]) {
        self.weekLabels = weekLabels
        super.init(frame: .zero)

        backgroundColor = UIColor(white: 0.12, alpha: 0.8)
        layer.cornerRadius = 8
        clipsToBounds = true

        markerLabel.translatesAutoresizingMaskIntoConstraints = false
        markerLabel.numberOfLines = 0
        markerLabel.font = .boldSystemFont(ofSize: 12)
        markerLabel.textColor = .white

        addSubview(markerLabel)

        NSLayoutConstraint.activate([
            markerLabel.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            markerLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -8),
            markerLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            markerLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10)
        ])
    }

    required init?(coder: NSCoder) {
        return nil
    }

    func update(weekLabels: [String]) {
        self.weekLabels = weekLabels
    }

    override func refreshContent(entry: ChartDataEntry, highlight: Highlight) {
        let clampedIndex = max(0, min(Int(entry.x.rounded()), weekLabels.count - 1))
        let weekLabel = weekLabels[safe: clampedIndex] ?? ""
        let datasetLabel = chartView?.data?.dataSets[safe: highlight.dataSetIndex]?.label ?? AppStrings.priceLabel
        let valueLabel = GasPriceTransformer.formatPrice(entry.y)

        markerLabel.text = "\(datasetLabel)\n\(weekLabel)\n\(valueLabel) EUR/l"
        layoutIfNeeded()

        let size = systemLayoutSizeFitting(UIView.layoutFittingCompressedSize)
        bounds.size = size

        super.refreshContent(entry: entry, highlight: highlight)
    }

    override func offsetForDrawing(atPoint point: CGPoint) -> CGPoint {
        CGPoint(x: -bounds.width / 2.0, y: -bounds.height - 16.0)
    }
}