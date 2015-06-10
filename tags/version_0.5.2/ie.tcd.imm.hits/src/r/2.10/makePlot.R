## Wrapper around the function 'fun' which opens first a png and later
## a pdf device before its execution. All the plotting is supposed to
## be handled by 'fun', and its result will be passed on to the
## calling function.
##
## path:          The file path where to create the image files
## name:          The file name which will be appended by '.pdf' and '.png',
##                respectively
## w, h:          The width and height of the device in inches.
## fun:           the function performing the actual plotting
## psz:           The pointsize used for the pdf device
## pdfArgs:       List of additional arguments when calling the fun with the 
##                pdf device open.
## pngArgs:       List of additional arguments when calling the fun with the 
##                png device open.
## thumbFactor:   A numeric vector giving a factor by which the thumbnail
##                png version of the plot should be shrunk. Can be of length 2
##                in which case the first scalar refers to width and the second
##                to height. E.g., a value of two will shrink the thumbnail to
##                half the size of the pdf.
## ...:           Additional arguments that will be passed on to 'fun' 
makePlot <- function(path, name, w, h, fun, psz=12,
                     thumbPsz=12, font="Helvetica", pdfArgs=list(),
                     pngArgs=list(), thumbFactor=1, ...) 
{
    ## The pdf version first
    thumbFactor <- rep(thumbFactor, 2)
    outf <- paste(name, c("pdf", "png"), sep=".")
    pdf(file.path(path, outf[1]), width=w, height=h, pointsize=psz, font=font)
    do.call(fun, args=append(pdfArgs, list(...)))
    dev.off()
    ## Now the png thumbnail
    nrppi <- 72
    wd <- (w*nrppi)/thumbFactor[1]
    hg <- (h*nrppi)/thumbFactor[2]
    png(file.path(path, outf[2]), width=wd, height=hg, pointsize=thumbPsz)
    res <- do.call(fun, args=append(pngArgs, list(...)))
    dev.off()
    return(res)
}
