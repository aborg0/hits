## The workhorse function for the 'Plate Summaries' module: boxplots of raw and normalized
## data as well as controls plots.
writeHtml.experimentQC <- function(cellHTSList, module, con, allControls, allZfac)
{
    outdir <- dirname(module@url)
    xn <- cellHTSList$normalized
    xr <- cellHTSList$raw
    writeHtml.header(con)
    QMexperiment(xn, xr, outdir, con, allControls, allZfac)
    writeHtml.trailer(con)
}

## The main workhorse function to produce the experiment-wide QC report. In particular,
## this includes boxplots of raw data ad normalized data as well as dot plots and density
## plots of the control distributions.
QMexperiment <- function(xn, xr, path, con, allControls, allZfac)
{
    ## Initialize dimensions and state variables from the raw data and (if available)
    ## normalized data objects
    hasNormData <- !(is.null(xn))
    nrbxp <- 1 + hasNormData
    nrCh <- ifelse(hasNormData, dim(Data(xn))[3], dim(Data(xr))[3])
    channelNames <- if(!is.null(xn)) channelNames(xn) else channelNames(xr)
    nrPlate <- max(plate(xr))
    nrReplicate <- dim(xr)[2]
    nrWell <- prod(pdim(xr))
    plt <- plate(xr)

    ## Checks whether the number of channels has changed (e.g. normalized data)
    hasLessCh <- if(hasNormData) (dim(Data(xr))[3] > dim(Data(xn))[3]) else FALSE
    
    ## Get positions for all of the controls if there are any
    nrPos <- nrNeg <- 0
    if(hasNormData | state(xr)["configured"])
    {  
        actCtrls <- allControls$actCtrls
        inhCtrls <- allControls$inhCtrls
        posCtrls <- allControls$posCtrls
        negCtrls <- allControls$negCtrls
        nrPos <- sapply(actCtrls, length) + sapply(inhCtrls, length) +
            sapply(posCtrls, function(w) length(unlist(w)))
        nrNeg <- sapply(negCtrls, length) 		
    }
    		
    ## Create a dataframe for the plots of each channel
    plotTable <- data.frame(matrix(data=NA, nrow=0, ncol=nrCh + 1))
    names(plotTable) <- c("", paste("Channel", 1:nrCh, sep=" "))
    chList <- vector(mode="list", length=nrCh)
    names(chList) <- channelNames
    for(ch in 1:nrCh)
    {
        repList <- list()
        for (r in 1:nrReplicate)
        {
            ## batch information
            btr <- if(!is.null(batch(xr)))
                {
                    if(hasNormData)
                    {
                        batch(xn)[,r]
                    }
                    else
                    {
                        batch(xr)[,r]
                    }
                }
            else rep(1L, nrPlate)
            
            ## Create the boxplot of measurement values before and
            ## after normalization (if applicable)
            settings <- chtsGetSetting(c("plateSummaries", "boxplot"))
            width <- settings$size*(nrbxp-hasLessCh)
            height <- 4.5
            makePlot(path, name=sprintf("boxplot_%d_%d", r, ch), w=width, h=height,
                     font=settings$font, thumbFactor=settings$thumbFactor,
                     psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                     col=settings$col, fun = function(col, ...)
                 {
                     par(mfrow=c(1, (nrbxp-hasLessCh)), mai=c(0.8, 0.8, 0.2, 0.2),
                         mgp=c(2.5,1,0))
                     col <- rep(col,2)
                     if (!hasLessCh)
                     {
                         xbp <- matrix(Data(xr)[,r,ch], ncol=nrPlate, nrow=nrWell)
                         boxplotwithNA(xbp, col=col[1], outline=FALSE, main="", xlab="plate",
                                       ylab="raw value", batch=btr)
                     }
                     if(hasNormData)
                     {
                         xbp <- matrix(Data(xn)[,r,ch], ncol=nrPlate, nrow=nrWell)
                         boxplotwithNA(xbp, col=col[2], outline=FALSE, main="", xlab="plate",
                                       ylab="normalized value", batch=btr)
                     }
                 })
            caption <- 
                if(hasNormData & !hasLessCh)
                    sprintf("Left: raw, right: normalized", r)
                else
                    NA
            img <- sprintf("boxplot_%d_%d.png", r, ch)
            title <- "Boxplot"
            
            ## Create the controls plot if there are any controls
            missingError <- NULL
            if( nrPos[ch] & nrNeg[ch] & hasNormData )
            {
                xbp <- matrix(Data(xn)[,r,ch], ncol=nrPlate, nrow=nrWell)
                yvals <- lapply(posCtrls[[ch]], function(d) xbp[d])
                yvals$neg <- xbp[negCtrls[[ch]]]
                yvals$inh <- xbp[inhCtrls[[ch]]]
                yvals$act <- xbp[actCtrls[[ch]]]
                if (!all(is.na(unlist(yvals))))
                {
                    settings <- chtsGetSetting(c("plateSummaries", "controls"))
                    width <- settings$size*(nrbxp)
                    height <- 4.5
                    makePlot(path, name=sprintf("Controls_%d_%d", r, ch),
                             w=width, h=height,
                             font=settings$font, thumbFactor=settings$thumbFactor,
                             psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                             fun = function(...)
                         {
                             par(mfrow=c(1, nrbxp), mai=c(0.8, 0.8, 0.2, 0.2),
                                 mgp=c(2.5,1,0))
                             xvals <- lapply(posCtrls[[ch]], function(d) plt[d]) 
                             xvals$neg <- plt[negCtrls[[ch]]]
                             xvals$inh <- plt[inhCtrls[[ch]]]
                             xvals$act <- plt[actCtrls[[ch]]]
                             controlsplot(xvals, yvals, main="", batch=btr)
                             ## density function needs at least 2 points
                             ## dealing with the case where we have a single positive or negative
                             ## control well, and a single plate, so that a single measurement
                             ## is available in either xpos or xneg or both.
                             yvals <- lapply(yvals, function(d) d[!is.na(d)])
                             yvals.len <- sapply(yvals, length)>1
                             if (yvals.len[["neg"]] & sum(yvals.len)>1) {
                                 densityplot(values=yvals,
                                             zfacs= sapply(names(allZfac),
                                             function(i) allZfac[[i]][r,ch]), main="")
                             }
                         })					
                    img <- c(img, sprintf("Controls_%d_%d.png", r, ch))
                    title <- c(title, "Controls Plot")
                    caption <- c(caption, NA)
                }
                else
                {## if !all NA
                    missingError <- "Values for 'pos' and 'neg' controls are missing."
                }## else !allNA
            }
            else
            {## if nrPos & nrNeg
                missingError <- paste("No controls ('pos' and 'neg') were found",
                                      "and/or the 'cellHTS' object is not normalized yet.")
            }## else nrPos nrNeg
            htmlImg <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                            fullImage=gsub("png$", "pdf", img), caption=caption))
            repList <- append(repList, htmlImg)
        } ## for r
        chList[[ch]] <- repList
    } ## for ch
    stack <- chtsImageStack(chList, id="expQC")
    writeHtml(stack, con=con, vertical=TRUE)
    return(invisible(NULL))
}



boxplotwithNA <- function(x, batch, ...)
{
    if(!all(is.na(x)))
    {
        sel <- apply(x,2,function(x) all(is.na(x)))
        bc <- rep(1, ncol(x))
        bc[sel] <- NA
        xsp <- split(x, col(x))
        bp <- boxplot(xsp, plot=FALSE)
        border <- IQR(x, na.rm=TRUE)/10
        lowerLim <- min(bp$stats[1,], na.rm=TRUE)-border
        upperLim <- max(bp$stats[5,], na.rm=TRUE)+border
        boxplot(xsp, ..., ylim=c(lowerLim, upperLim), border=bc)
        if(ncol(x)==1)
            axis(1, 1)
        bdiff <- diff(batch)
        if(sum(bdiff, na.rm=TRUE)>0)
        {
            ind <- 1:length(batch)
            abline(v=which(as.logical(bdiff))+0.5, lty=1)
        }
    }
    else
    {
        args <- list(...)
        tks <- 1:ncol(x)
        plot(tks, tks, type="n", axes=FALSE, xlab=args$xlab,
             ylab=args$ylab, xlim=c(0, max(tks)), main=args$main)
        axis(1, at=tks-0.5, labels=tks)
        box()
    }
}



colors4Controls <- function(vals)
{
    len.x <- length(vals)
    cols <- chtsGetSetting("controls")$col[c("neg", "act", "inh")] 
    #    c(neg="#2040FF", act="#E41A1C", inh="#FFFF00")  
    if (len.x>3)
    {
        Lab.pal <- colorRampPalette(c("darkred", "red", "orange"), space="Lab")(len.x-3)
        names(Lab.pal) <- names(vals)[! (names(vals) %in% names(cols))] 
        cols <- append(cols, Lab.pal)
        if ("pos" %in% names(cols) & len.x==4)
            cols["pos"]="#E41A1C"
    }
    return(cols)
}



densityplot <- function(values, zfacs, ...)
{
    dens <- list()
    ymax <- xmax <- xmin <- numeric(0)
	
    cols <- colors4Controls(values)
    sel <- sapply(values, length)
    values <- values[sel>1]
	
    zfacs <- zfacs[!is.na(zfacs)]
    for(i in 1:length(values))
    {
        theDens <- density(values[[i]], na.rm=TRUE, adjust=4)
        ymax <- max(ymax, theDens$y)
        xmax <- max(xmax, theDens$x)
        xmin <- min(xmin, theDens$x)
        dens[[i]] <- theDens
        names(dens)[i] <- names(values)[i]
    }
    plot(dens[[1]], xlim = c(xmin, xmax), ylim=c(0, ymax*1.2), col=cols[names(dens)[1]],
         yaxt="n",ylab="",
         xlab="normalized value", ...)
    for(i in 2:length(dens))
        lines(dens[[i]], col=cols[names(dens)[i]])
    legend("top", legend=paste("'", names(dens), "' controls", sep=""), pch=16,
           col=cols[names(dens)],
           bg="white", cex=0.7, title = paste(sprintf("Z'-factor (%s) = %g", names(zfacs),
                                round(zfacs,2)),
                                collapse=" "), horiz=TRUE, pt.cex=0.5, bty="n") 
}



controlsplot <- function(xvals, yvals, batch, ...)
{
    ylim <- range(unlist(yvals), na.rm=TRUE, finite=TRUE)
    inc <- 0.2*diff(ylim)
    ylim <- ylim+c(-inc, inc)
    cols <- colors4Controls(xvals)
    sel <- sapply(xvals, length)
    xvals <- xvals[sel!=0]
    yvals <- yvals[sel!=0]
    stopifnot(names(xvals) == names(yvals))
	
    plot(xvals[[1]], yvals[[1]], pch=16, cex=0.5, ylim=ylim, xlab="plate",
         ylab="normalized value",
         col=cols[names(xvals)[1]], xaxt="n", ...)
    legend("top",legend=paste("'", names(xvals), "' controls", sep=""), col=cols[names(xvals)],
           horiz=TRUE, pch=16, pt.cex=0.5, bg="white", cex=0.7, bty="n")
    xall <- split(unlist(yvals), unlist(xvals))
    xall <- xall[!sapply(xall, function(f) all(is.na(f)))]
    xalls <- data.frame(lapply(xall, function (k) range(k, na.rm=TRUE)))
    segments(as.numeric(names(xall)), as.matrix(xalls)[1,], as.numeric(names(xall)),
             as.matrix(xalls)[2,], lty=3)
    for(i in 2:length(xvals))
        points(xvals[[i]], yvals[[i]], pch=16, cex=0.5, col=cols[names(xvals)[i]])
    mp <- max(unlist(xvals))
    by <- if ((mp-1)%/%20) 10 else ifelse((mp-1)%/%10, 5, 1) 
    axis(1, at = c(1, seq(0,mp,by=by)[-1]), labels = TRUE)
	
    batch <- batch[unique(unlist(xvals))] 
    bdiff <- diff(batch)
    if(sum(bdiff, na.rm=TRUE)>0)
    {
        ind <- unique(unlist(xvals))
        abline(v=ind[which(as.logical(bdiff))]+0.5, lty=1)
    } 
    return(list(xvals, yvals))
}
