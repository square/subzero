/**
 * go run dvd_label.go
 */

package main

import (
  "fmt"
  "time"

	"github.com/jung-kurt/gofpdf"
  "gopkg.in/alecthomas/kingpin.v2"
)

var (
  outline   = kingpin.Flag("outline", "draw outline (useful for debugging).").Bool()
  ver = kingpin.Flag("ver", "version string").Required().String()
)

func main() {
  kingpin.Version("0.0.1")
  kingpin.Parse()

	var pdf *gofpdf.Fpdf
  // create a blank PDF. P=portrait.
	pdf = gofpdf.New("P", "mm", "Letter", "")
	pdf.AddPage()
  pdf.SetMargins(0, 0, 0)
	pdf.SetFont("Arial", "B", 16)

  // top
  if *outline {
    // innner
    pdf.Circle(105 + 3, 70 + 7, 20, "D")
    // outer
    pdf.Circle(105 + 3, 70 + 7, 60, "D")
  }

  // text
  pdf.SetXY(0, 70 + 7 - 40)
  pdf.WriteAligned(0, 10, "Plutus", "C")
  pdf.SetXY(0, 70 + 7 + 30)
  now := time.Now()
  pdf.WriteAligned(0, 10, now.Format("Jan 2, 2006"), "C")
  pdf.SetXY(0, 70 + 7 + 40)
  pdf.WriteAligned(0, 10, fmt.Sprintf("(%s)", *ver), "C")

  // logo
  var opt gofpdf.ImageOptions
  pdf.ImageOptions("../logo.png", 105 + 3 + 37 - 10, 70 + 7 - 10, 25, 25, false, opt, 0, "")

  // bottom
  if *outline {
    // innner
    pdf.Circle(105 + 3, 196 + 7, 20, "D")
    // outer
    pdf.Circle(105 + 3, 196 + 7, 60, "D")
  }

  // text
  pdf.SetXY(0, 196 + 7 - 40)
  pdf.WriteAligned(0, 10, "Plutus", "C")
  pdf.SetXY(0, 196 + 7 + 30)
  pdf.WriteAligned(0, 10, now.Format("Jan 2, 2006"), "C")
  pdf.SetXY(0, 196 + 7 + 40)
  pdf.WriteAligned(0, 10, fmt.Sprintf("(%s)", *ver), "C")

  // logo
  pdf.ImageOptions("../logo.png", 105 + 3 + 37 - 10, 196 + 7 - 10, 25, 25, false, opt, 0, "")


  err := pdf.OutputFileAndClose("dvd_label.pdf")
  fmt.Printf("Writing dvd_label.pdf")
  if err != nil {
    fmt.Println("error: %s", err)
  }
}
