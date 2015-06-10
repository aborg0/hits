## The workhorse function for the 'Plate Configuration' module. This will create the
## image plot indicating the position of controls and samples on the plates (currently
## only based on the content of plate configuration file! No updates based on screen
## log file) and wrap the result in appropriate HTML code. 
writeHtml.plateConf <- function(cellHTSList, module, nrPlate, posControls,
                                negControls, con)
{
    outdir <- dirname(module@url)
    xr <- cellHTSList$raw
    if(state(xr)[["configured"]] && chtsGetSetting(c("plateConfiguration", "include")))
    {
        ## Create the image plots of the plate configuration as jpg and pdf
        fnam <- "configurationAsScreenPlot"
        settings <- chtsGetSetting("plateConfiguration")
        height <- settings$size*pdim(xr)["nrow"]/pdim(xr)["ncol"]*ceiling(nrPlate/6)/6+0.5
        res <- makePlot(outdir, name=fnam, w=settings$size, h=height,
                        font=settings$font, thumbFactor=settings$thumbFactor,
                        psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                        pdfArgs=list(legend=TRUE,
                        main=sprintf("Plate Configuration for screen '%s'", name(xr))),
                        maxRasters=nrPlate+10, fun=function(...)
                    {
                        do.call(fnam, 
                                args=list(x=xr, verbose=FALSE,
                                posControls=unlist(posControls),
                                negControls=negControls, ...))
                    })
        ## Wrap as chtsImage object to get nice HTML layout
        img <- chtsImage(data.frame(thumbnail=paste(fnam, "png", sep="."),
                                    fullImage=paste(fnam, "pdf", sep="."),
                                    title="Plate Configuration"))
        ## A color legend for the plot
        mat <- matrix(NA, ncol=length(res), nrow=1)
        mat <- rbind(mat, names(res))
        img@additionalCode <- hwrite(mat, border=FALSE, bgcolor=rbind(res, NA),
                                     center=TRUE, table.class="plateConfModule legend",
                                     class="plateConfModule legend",
                                     style="width:30px; border: 1px solid #6699cc;")
        ## Now we produce the necessary HTML
        writeHtml.header(con)
        writeHtml(img, con=con)
        writeHtml.trailer(con)
        return(NULL)
    }
    else
    {
        return(NA)
    }
}



## Function that shows the plate configuration as an image screen. The
## default color code is similar to that used in writeReport and it
## will be returned by the function
configurationAsScreenPlot <- function(x, verbose=interactive(), posControls, negControls,
                                      legend=FALSE, main="")
{
    ## optional inputs: names of 'pos' and 'neg' controls given as vectors of
    ## regular expressions
    ## initial checks:
    if(!is(x, "cellHTS"))
        stop("'x' should be of class 'cellHTS'.")
    ## Check the status of the 'cellHTS' object
    if(!state(x)[["configured"]])
        stop("Please configure 'x' (using the function 'configure') before normalization.")

    wellAnnotation <- as.character(wellAnno(x))
    wellCols <- c(sample="#999999", other="#000000", empty="#FFFFFF")
    pcolPal <- rev(brewer.pal(9, "Reds")[-c(1,2,9)])
    ncolPal <- rev(brewer.pal(9, "Blues")[-c(1,2,9)])
    if(missing(negControls)) negControls <- "^neg$"
    if(missing(posControls)) posControls <- "^pos$"
    ## this gives the index in wellAnno(x)
    negInd <- findControls(negControls, as.character(wellAnno(x)))  
    posInd <- findControls(posControls, as.character(wellAnno(x)))
    aux <- if(is.list(posInd)) sapply(posInd, length)==0 else length(posInd)==0
    if(any(aux) && verbose)
        warning(sprintf("'%s' not found among the well annotation!\n",
                        posControls[which(aux)]))
    aux <- if(is.list(negInd)) sapply(negInd, length)==0 else length(negInd)==0
    if(any(aux) && verbose)
        warning(sprintf("'%s' not found among the well annotation!\n",
                        negControls[which(aux)]))
    namePos <- unique(sapply(posInd, function(i) unique(wellAnnotation[i])))
    nameNeg <- unique(sapply(negInd, function(i) unique(wellAnnotation[i])))
    ## update well colors and update well colors with pos and neg controls
    if(length(namePos))
    {
        cols.pos <- if(length(namePos)==1) pcolPal[1] else colorRampPalette(pcolPal,
                             space="Lab")(length(namePos))
        names(cols.pos) <- namePos
        wellCols <- c(wellCols, cols.pos)
    }
    if(length(nameNeg))
    {
        cols.neg <-
            if(length(nameNeg)==1) ncolPal[1] else colorRampPalette(ncolPal,
                     space="Lab")(length(nameNeg))
        names(cols.neg) <- nameNeg
        wellCols <- c(wellCols, cols.neg)
    }
    ## remove unused well annotation
    wellCols <- wellCols[names(wellCols) %in% unique(wellAnnotation)]
    mtW <- match(wellAnnotation, names(wellCols))
    wh <- is.na(mtW)
    if(length(wh)>0)  {
        notCovered <- unique(wellAnnotation[wh])
        notCov <- substr(rainbow(length(notCovered)), 1, 7)
        names(notCov) <- notCovered
        wellCols <- c(wellCols, notCov)
        mtW[wh] <- match(wellAnnotation[wh], names(wellCols))
      }
    mtW = factor(mtW)
    levels(mtW) = names(wellCols)
    
    plotScreen(split(mtW, plate(x)),
               fill = wellCols,
               do.legend = legend,
               main = main,
               nx = pdim(x)[["ncol"]],
               ny = pdim(x)[["nrow"]],
               ncol = ifelse(max(plate(x)) < 6L, max(plate(x)), 6L))
    
    ## put correct names (name given in the conf file)
    aux <- sapply(names(wellCols), function(i)
                  plateConf(x)$Content[match(i, tolower(plateConf(x)$Content))]) 
    if(any(is.na(aux)))
        aux[is.na(aux)] <- names(wellCols)[is.na(aux)]
    names(wellCols) <- aux
    invisible(wellCols)
}
