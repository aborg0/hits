## Wrapper around the function 'fun' which opens first a png and later a pdf device
## before its execution.
makePlot <- function(path, con, name, w, h=devDims(w)$height, fun, psz=12, print=TRUE, 
                     isPlatePlot=FALSE, isImageScreen=FALSE) 
{
    
    outf <- paste(name, c("pdf", "png"), sep=".")
    nrppi <- 72

    pdf(file.path(path, outf[1]), width=w, height=h, pointsize=psz)
    if (isImageScreen) fun(map=FALSE) else fun()
    dev.off()

    if (isPlatePlot) 
    {
        wd <- devDims(w)$pwidth
        hg <- devDims(w)$pheight
    } 
    else 
    {
        wd <- w*nrppi
        hg <- h*nrppi
    }
    
    png(file.path(path, outf[2]), width=wd, height=hg, pointsize=psz)
    res <- fun()
    dev.off()

    if (print)
        cat(sprintf("<CENTER><IMG SRC=\"%s\"/></CENTER><BR>\n",
                    outf[2]), file=con)
    return(res)
}
