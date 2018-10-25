/**
 * go run dvd_label.go --ver 206
 */

package main

import (
	"fmt"
	"time"

	"github.com/jung-kurt/gofpdf"
	"gopkg.in/alecthomas/kingpin.v2"
)

var (
	outline = kingpin.Flag("outline", "draw outline (useful for debugging).").Bool()
	ver     = kingpin.Flag("ver", "version string").Required().String()
)

func main() {
	kingpin.Version("0.0.1")
	kingpin.Parse()

	var pdf *gofpdf.Fpdf
	// create a blank PDF. P=portrait.
	pdf = gofpdf.New("P", "mm", "Letter", "")
	pdf.AddFont("SQMarket", "", "./SQMarket-Regular.json")
	pdf.AddPage()
	pdf.SetMargins(0, 0, 0)
	pdf.SetTextColor(130, 166, 185)

	now := time.Now()

	// top
	draw(pdf, now, 105, 70)

	// bottom
	draw(pdf, now, 105, 196)

	err := pdf.OutputFileAndClose("dvd_label.pdf")
	if err != nil {
		fmt.Printf("error: %s\n", err)
		return
	}
	fmt.Printf("Done writing dvd_label.pdf\n")
}

func draw(pdf *gofpdf.Fpdf, now time.Time, x float64, y float64) {
	// bottom
	if *outline {
		// innner
		pdf.Circle(x+3, y+7, 20, "D")
		// outer
		pdf.Circle(x+3, y+7, 60, "D")
	}

	// text
	pdf.SetFont("SQMarket", "", 40)
	pdf.SetXY(0, y+7-40)
	pdf.WriteAligned(0, 10, "Subzero", "C")

	pdf.SetFont("SQMarket", "", 12)
	pdf.SetXY(0, y+7+30)
	pdf.WriteAligned(0, 10, now.Format("Jan 2, 2006"), "C")
	pdf.SetXY(0, y+7+35)
	pdf.WriteAligned(0, 10, fmt.Sprintf("(%s)", *ver), "C")

	// logo
	var opt gofpdf.ImageOptions
	pdf.ImageOptions("logo.png", x+3+37-10, y+7-10, 25, 25, false, opt, 0, "")
}
