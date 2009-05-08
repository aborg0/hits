## The workhorse function for the 'Plate List' module: this is a matrix of quality metrics
## for the different plates and links to the per plate quality reports. 
writeHtml.plateList <- function(cellHTSList, module, exptab, links, center, glossary,
                                outdir, htmldir, configured, con, ...)
{
    ## Copy the original intensity files for the compedium and also generate HTML files
    nn <- writeIntensityFiles(outdir=outdir, xr=cellHTSList$raw, htmldir=htmldir)
    ## Now the QC results
    writeHtml.header(con, path="../html")
    links[!is.na(links[, "Filename"]),"Filename"] <- nn
    sel <- !is.na(links[, "status"])
    links[sel,"status"] <- file.path("../", links[sel,"status"])
    
    writeQCTable(exptab, url=links, con=con, glossary=glossary, configured=configured,
                 xr=cellHTSList$raw)
    writeHtml.trailer(con)
    return(NULL)
}


## Write raw data files from the cellHTS object back to disk, both as regular
## txt files and as formatted HTML 
writeIntensityFiles <- function(outdir, xr, htmldir)
{
    wh <- which(plateList(xr)$status=="OK")
    nm <- file.path("in", names(intensityFiles(xr)))
    newFileNames <- NULL
    for(w in wh)
    {
        txt <- intensityFiles(xr)[[w]]		
        if(is.null(txt))
            stop(sprintf(paste("CellHTS object is internally inconsistent, plate %d",
                               "(%s) is supposedly OK but has no raw data file."),
                         as.integer(w), nm[w]))
        writeLines(txt, file.path(outdir, nm[w]))
        html <- gsub("in/", "", paste(strsplit(nm[w], ".", fixed=TRUE)[[1]][1], "html", sep="."))
        con <- file(file.path(htmldir, html), open="w")
        writeHtml.header(con, path="../html")
	writeLines(sprintf("<p class=\"verbatim\">%s</p>", paste(txt, collapse="<br>")), con)
        writeHtml.trailer(con)
        close(con)
        newFileNames <- c(newFileNames, html)
    }
    return(newFileNames)
}



## Function to outogenerate a matrix of class labels from a data frame, where common rows as
## denoted by nrPlates alternate between 'odd' and 'even'
plateListClass <-  function(df, nrPlates, classes=c("odd", "even"))
{
    mclass <- matrix(classes[1], ncol=ncol(df), nrow=nrow(df))
    mclass[rep(seq_along(nrPlates)%%2==0, nrPlates)] <- classes[2]
    return(mclass)
}



## Function using hwriter to color a data.frame in a checkerboard way
## returns a matrix of colors
## dataframe : dataframe to be colored in the html table
dataframeColor <- function(dataframe, basicColors=matrix(c("#D5DDF3","#f0f0ff","#d0d0f0","#e0e0ff"),
                                      ncol=2, byrow=TRUE))
{     
    mcolor <- matrix(basicColors[1+(1:ncol(dataframe))%%2,1+(1:nrow(dataframe))%%2],ncol=ncol(dataframe),
                     nrow=nrow(dataframe), byrow=TRUE) 
    return(mcolor)
}



## The function creating the HTML table of QC scores including all links
writeQCTable <- function(x, url, glossary, configured, xr, con)
{
    ## The glossary
    if(!is.null(glossary))
    {
        cn <- colnames(x)
        common <- intersect(glossary$word, cn)
        rownames(glossary) <- glossary$word
        cn[match(common, cn)] <- paste("<span onmouseover=\"Tip('",
                                       glossary[colnames(x[common]),2],
                                       "', WIDTH, 250, TITLE, 'Definition', OFFSETX, 1)\"",
                                       " onmouseout=\"UnTip();\" onClick=\"if(tt_Enabled) ",
                                       "linkToFile('glossary.html');\">",
                                       colnames(x[common]),"</span>", sep="")
    }
    ## Finding the redundant plates
    ## hwriter does not allow for rowspans, so we have to fake an additional line
    ## in a separate table, all pretty ugly.
    red <- table(x$Plate)
    redHTML <- "
      <table class=\"plate\">
        <tr>
          <td>
        </td>
          <td class=\"header\">
            Plate
          </td>
        </tr>"
    curPlate <- 1
    class <- "odd"
    stat <- split(url[,"status"], rep(seq_along(red), as.integer(red)))
    stat <- sapply(stat, function(z) if(all(is.na(z))) NA else unique(z[!is.na(z)]))
    for(i in seq_along(red))
    {
        pl <- paste(sprintf("
          <td class=\"plate %s\">
            %s
          </td>
        </tr>", class, x$Plate[curPlate:(curPlate+red[i]-1)]),
                    collapse="\n      <tr>\n")
        redHTML <- c(redHTML, sprintf("
        <tr>
          <td rowspan=\"%s\" class=\"details\" onClick=\"linkToFile('%s')\"%s>
            &nbsp&nbsp&nbsp
          </td>
          %s", red[i], stat[i],
               addTooltip(sprintf("Detailed QC information for plate %s across all replicates and channels.", i),
                          "Help", FALSE), pl))
        curPlate <- curPlate+red[i]
        class <- if(class=="odd") "even" else "odd"
    }
    redHTML <- c(redHTML, "</tr></table>")
    if(configured)
    {
        x <- x[,-1]
        url <- url[,-1]
    }
    url[,"status"] <- NA
    em <- xr@plateList$errorMessage
    sel <- !is.na(em)
    if(any(sel))
        x[sel, "status"] <-
            sprintf("<span %s>%s</span>", addTooltip(xr@plateList$errorMessage[sel], "Details", FALSE),
                    x[sel, "status"])
    url <- rbind(NA, url)
    tabClasses <- rbind("header", plateListClass(x, red))
    fn <- which(colnames(url)=="Filename")
    tabClasses[-1,fn] <- paste(tabClasses[-1,fn], "link")
    x <- rbind(if(configured) cn[-1] else colnames(x), x)

    ## No need to produce output for empty lines
    empty <- apply(x[-1,], 2, function(z) all(z == "" | is.na(z)))
    if(any(empty))
    {
        x <- x[,!empty]
        tabClasses <- tabClasses[,!empty]
        url <- url[,!empty]
    }
    tabHTML <-  hwrite(x, row.names=FALSE, col.names=FALSE, class=tabClasses,
                       border=0, table.class="rest", link=url)
    out <- sprintf("
<table class=\"plateList\" align=\"center\">
  <tr>
    <td>
      %s
    </td>
    <td>
      %s
    </td>
  </tr>
</table>", ifelse(configured, paste(redHTML, collapse="\n"), ""), paste(tabHTML, collapse="\n"))
    writeLines(out, con)
}



## Create a HTML image map from a matrix of (rectangular) coordinates. Note that this will not create the
## whole img tag, but only the map part, which needs to be injected in the img.
myImageMap <- function(object, tags, imgname)
{		
    if(!is.matrix(object)||ncol(object)!=4)
        stop("'object' must be a matrix with 4 columns.")
    len <- lapply(tags, length)
    if(any(len != nrow(object)))
        stop(sprintf("Elements of the list 'tag' must have a length equal to the number of rows of 'object' (%g).",
                     nrow(object)))
    mapname <- paste("map", gsub(" |/|#", "_", imgname), sep="_")
    outin <- sprintf("usemap=\"#%s\"><map name=\"%s\" id=\"%s\">\n", mapname, mapname, mapname)
    out <- lapply(1:nrow(object), function(i) { 
        paste(paste("<area shape=\"rect\" coords=\"", paste(object[i,], collapse=","),"\"", sep=""),
              paste(" ", paste(names(tags), "=\"",c(tags[["title"]][i], tags[["href"]][i]),"\"", sep=""),
                    collapse=" "),
              " alt = \"\"/>\n", sep="")
    }) 	
    ## add all together:
    out <- paste(unlist(out), collapse="")
    out <- paste(outin, out, "</map",sep="")
    return(out)
}



## Create the per plate quality control page
QMbyPlate <- function(platedat, pdim, name, channelNames, basePath, subPath, genAnno, mt,
                      plotPlateArgs, brks, finalWellAnno, activators, inhibitors, positives,
                      negatives, isTwoWay, namePos, wellTypeColor, plateDynRange, plateWithData,
                      repMeasures)
{
    ## dimensions
    d <- dim(platedat)
    nrWells <- prod(pdim)
    nrChannel <- d[4]
    maxRep <- d[3]
	
    ## writing in the html report
    fn <- file.path(subPath, "index.html")
    con <- file(file.path(basePath, fn), open="w")
    on.exit(close(con))
    writeHtml.header(con, path="../html")
    ##hwrite(paste("Experiment report for", name), con, center = TRUE, heading = 1, br=TRUE) 
    ## hwrite("",con, br=TRUE)
	
    ## which of the replicate plates has not just all NA values
    whHasData <- list()
    for (ch in 1:nrChannel)
        whHasData[[ch]] <- which(plateWithData[,,ch])
    nrRepCh <- sapply(whHasData, length)
	
    ## Checks whether the number of channels has changed (e.g. normalized data)
    hasLessCh <- any(dim(finalWellAnno)!=dim(platedat))
	
    ## NOTE: 'subPath' corresponds to the plate number!
    ## Get well positions for current controls and for samples
    ppos <- pneg <- pact <- pinh <- list()
    if(isTwoWay)
    {
        for (ch in 1:nrChannel)
        {
            ## correct to be in range 1:nrWells
            pact[[ch]] <- activators[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells)
            ## correct to be in range 1:nrWells
            pneg[[ch]] <- negatives[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells)
            ## correct to be in range 1:nrWells 
            pinh[[ch]] <- inhibitors[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells) 
        }
    }
    else
    {  #oneWay
        for (ch in 1:nrChannel)
        {
            ## correct to be in range 1:nrWells
            pneg[[ch]] <- negatives[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells)
            ppos[[ch]] <- lapply(names(positives[[ch]]), function(i)
                                 positives[[ch]][[i]][[as.character(subPath)]]-(subPath-1)*(nrWells)) 
            names(ppos[[ch]]) <- names(positives[[ch]])
        }
    }
    
    samples <- which(mt==which(names(wellTypeColor)=="sample"))
	
    ## summary of the quality metrics in 'qm' to be returned by this function:
    qmsummary <- vector("list", length=nrChannel)
    names(qmsummary) = sprintf("Channel %d", 1:nrChannel)
	
    ## Create table with per-plate quality metrics
    for (ch in 1:nrChannel)
    {
        nrRep <- nrRepCh[ch]				
        ## 1) create summary table from dynamic range:
        d <- length(plateDynRange)*(maxRep + 1)
        qm <- data.frame(metric=I(character(d)), value=NA, comment=I(character(d)))
        for(i in 1:length(plateDynRange))
        {			
            pn <- if(names(plateDynRange)[i]=="pos" & length(plateDynRange)==1) "" else sprintf("'%s'",
                                                            names(plateDynRange)[i])
            qm$metric[(i-1)*(maxRep+1)+(1:maxRep)] <- I(sprintf("Dynamic range %s (replicate %s)",pn , 1:maxRep))
            qm$metric[(i-1)*(maxRep+1)+(maxRep+1)] <- I(sprintf("Dynamic range %s",pn))
            qm$value[(i-1)*(maxRep+1)+(1:maxRep)] <- round(plateDynRange[[i]][1, 1:maxRep, ch],2)
            qm$value[(i-1)*(maxRep+1)+(maxRep+1)] <- round(plateDynRange[[i]][1,maxRep+1, ch],2)
            hasNoVal <- is.na(plateDynRange[[i]][1,1:maxRep,ch]) 
            if(any(hasNoVal))
            {
                if(all(is.na(plateDynRange[[i]][1,,ch])))
                {
                    qm$comment[(i-1)*(maxRep+1) + (1:(maxRep+1))] <-
                        I(sprintf("No controls ('%s' and/or 'neg') were found.",
                                  ifelse(names(plateDynRange)[i] %in% c("activators", "inhibitors"),
                                         names(plateDynRange)[i], "pos"))) 
                }
                else
                {					
                    a <- intersect(hasNoVal, whHasData[[ch]]) 
                    if(length(a)) qm$comment[(i-1)*(maxRep+1) + a] <- I("No available values for one of the controls")
                    b <- setdiff(1:maxRep, whHasData[[ch]])
                    if(length(b))
                        qm$comment[(i-1)*(maxRep+1) + b] <- I(paste(paste("Replicate", b, sep=" "), "is missing", sep=" "))
                } # else all(is.na...
            } #  any(hasNoVal)
        } # for i in 1:length(plateDynRange)		
        ## 2. Correlation coefficient (just for samples wells)
        comm <- ""    
        if(nrRep>1) { ## subPath corresponds to the plate number
            cc1 <- round(repMeasures$repStDev[subPath,ch],2)
            cc2 <- if(maxRep==2) round(repMeasures$corrCoef[subPath,ch],2) else
            paste(round(repMeasures$corrCoef.min[subPath,ch],2), round(repMeasures$corrCoef.max[subPath,ch],2), sep=" - ")
        }
        else
        {
            cc1 <- cc2 <- as.numeric(NA)
            comm <- sprintf("%d replicate%s", nrRep, ifelse(nrRep, "", "s"))
        }
        qm <- rbind(qm, data.frame(metric=I("Repeatability standard deviation"), value=cc1, comment=I(comm)))
        qm <- rbind(qm, data.frame(metric=I(sprintf("Spearman rank correlation %s", ifelse(maxRep==2,"","(min - max)"))),
                                   value=cc2, comment=I(comm)))		
            
        ## store data in qmsummary 
        qmsummary[[ch]] <- qm$value
        names(qmsummary[[sprintf("Channel %d", ch)]]) <- qm$metric
    } # for ch
        	
    ## ------------------  Color legend for each channel ----------------------------------
    ## For the original configuration plate corrected by the screen log information:
    wellCount <- data.frame(matrix(NA, ncol = nrChannel, nrow = 2))
    names(wellCount) <- sprintf("Channel %d", 1:nrChannel)
    mtt <- vector("list", length = nrChannel)
    iwells <- match(c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh"), names(wellTypeColor))
    names(iwells) <- c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh")
	
    if (hasLessCh & nrChannel==1)
    {
        ## The color code must take into account the common entries between channels and replicates 		
        mtt[[1]] <- mt
        fwa <- matrix(finalWellAnno, ncol = prod(dim(finalWellAnno)[3:4]))
        mtrep <- apply(fwa, 2, function(u) match(u, names(wellTypeColor)))
        ## include the controls that were not annotated as "neg" or "pos":
        if (isTwoWay)
        {
            mtrep[pact[[1]],] [which(is.na(mtrep[pact[[1]],]))] <- iwells[["act"]]
            mtrep[pinh[[1]],] [which(is.na(mtrep[pinh[[1]],]))] <- iwells[["inh"]]
        }
        else
        {
            mtrep[unlist(ppos[[1]]),] [which(is.na(mtrep[unlist(ppos[[1]]),]))] <- iwells[["pos"]]
        }
        mtrep[pneg[[1]],] [which(is.na(mtrep[pneg[[1]],]))] <- iwells[["neg"]]
		
        ## replace the remaining NA positions by "other" (these corresponds to wells that
        ## although annotated as controls in the configuration file, don't behave as
        ## controls in the current channel
        mtrep[which(is.na(mtrep))] <- iwells[["other"]]
        aa <- apply(fwa, 2, function(u) sum(u=="flagged"))
        aa <- order(aa, decreasing=TRUE) # position 1 contains the replicate with more flagged values
        nrWellTypes <- sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))
		
        ## flagged wells
        wellCount[1,1] <- paste(sprintf("flagged: %d", nrWellTypes[iwells[["flagged"]]]), collapse=", ")
        ## all the other wells, except controls		
        fontColor <- wellTypeColor[-c(iwells[["flagged"]],iwells[["controls"]])]
        names <- names(wellTypeColor)[-c(iwells[["flagged"]],iwells[["controls"]])]
        nbr <- nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]			
        wellCount[2, 1] <- paste(sprintf("<font color=\"%s\">%s: %d</font>", fontColor, names, nbr),
                                 collapse="&nbsp&nbsp&nbsp")
		
        ## so "flagged" always wins over "pos", "neg" or "sample"
        mtt[[1]][is.na(mtt[[1]])] <- apply(mtrep[is.na(mtt[[1]]),, drop=FALSE], 1, max) 
        ## so "controls" always wins over "pos" or "neg" or "act" or "inh" or "sample"
        mtt[[1]][!is.na(mtt[[1]])] <- apply(mtrep[!is.na(mtt[[1]]),, drop=FALSE], 1, max)
		
    }
    else
    { ## if hasLessCh
        for (ch in 1:nrChannel)
        {
            mtt[[ch]] <- mt
            mtrep <- apply(finalWellAnno[,,,ch, drop=FALSE], 3, match, names(wellTypeColor))
			
            ## include the controls that were not annotated as "neg" or "pos":
            ## correct 'pos' controls just for one-way assays
            if (!isTwoWay)
            {
                if (length(unlist(ppos[[ch]])))
                {
                    mtrep[unlist(ppos[[ch]]),][which(is.na(mtrep[unlist(ppos[[ch]]),]))] <- iwells[["pos"]]
                }
                else
                { ## if length pos
                    ## replace possible wells annotated as "pos" by NA, because they shouldn't
                    ## be considered as a positive control for this channel:
                    if (any(mtt[[ch]] %in% iwells[["pos"]])) {
                        mtrep[mtt[[ch]] %in% iwells[["pos"]],] <- NA
                        mtt[[ch]][mtt[[ch]] %in% iwells[["pos"]]] <- NA 
                    } ## if any
                } ## else length pos
            }
            else
            { ## if !isTwoWay
				
                ## include the controls that were not annotated as "act" or "neg", but only if they
                ## should be regarded as such in this channel
                if (length(pact[[ch]]))
                {
                    mtrep[pact[[ch]],][which(is.na(mtrep[pact[[ch]],]))] <- iwells[["act"]]
                }
                else
                {## if length act
                    if (any(mtt[[ch]] %in% iwells[["act"]]))
                    {
                        mtrep[mtt[[ch]] %in% iwells[["act"]],] <- NA
                        mtt[[ch]][mtt[[ch]] %in% iwells[["act"]]] <- NA 
                    } ## if any
                } ## else length act
                if (length(pinh[[ch]]))
                {
                    mtrep[pinh[[ch]],] [which(is.na(mtrep[pinh[[ch]],]))]=iwells[["inh"]]
                }
                else
                {## if length inh
                    if (any(mtt[[ch]] %in% iwells[["inh"]]))
                    {
                        mtrep[mtt[[ch]] %in% iwells[["inh"]],] <- NA
                        mtt[[ch]][mtt[[ch]] %in% iwells[["inh"]]] <- NA 
                    } ## if any
                } ## else length inh
            }##else if !isTwoWay
			
            ## for the negative controls
            if (length(pneg[[ch]]))
            {
                mtrep[pneg[[ch]],] [which(is.na(mtrep[pneg[[ch]],]))] <- iwells[["neg"]] 
            }
            else
            { ## if length neg
                if (any(mtt[[ch]] %in% iwells[["neg"]]))
                {
                    mtrep[mtt[[ch]] %in% iwells[["neg"]],] <- NA
                    mtt[[ch]][mtt[[ch]] %in% iwells[["neg"]]] <- NA 
                } ## if any
            } ## else length neg
            
            ## replace the remaining NA positions by "other" (these corresponds to wells that
            ## although annotated as controls in the configuration file, don't behave as controls
            ## in the current channel
            mtrep[which(is.na(mtrep))] <- iwells[["other"]]
            aa <- apply(finalWellAnno[,,,ch, drop=FALSE], 3, function(u) sum(u=="flagged"))
            aa <- order(aa, decreasing=TRUE)
            nrWellTypes <- sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))
			
            ## flagged wells
            wellCount[1,ch] <- if(nrWellTypes[iwells[["flagged"]]])
                paste(sprintf("(%d flagged samples)", nrWellTypes[iwells[["flagged"]]]), collapse=", ") else ""
            ## the other wells, except controls
            fontColor <- wellTypeColor[-c(iwells[["flagged"]], iwells[["controls"]])]
            names <- names(wellTypeColor)[-c(iwells[["flagged"]],iwells[["controls"]])]
            nbr <- nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]
            wellCount[2, ch] <- paste(sprintf("<font color=\"%s\">%s: %d</font>", fontColor, names, nbr),
                                      collapse="&nbsp&nbsp&nbsp")
            ## so "flagged" always wins over "pos", "neg" or "sample" or "act" or "inh"
            mtt[[ch]][is.na(mtt[[ch]])] <- apply(mtrep[is.na(mtt[[ch]]),, drop=FALSE], 1, max)
        }## for channel
    }## else hasLessCh

    ##  ------------------  Make plots ----------------------------------
    chList <- vector(mode="list", length=nrChannel)
    plsiz <- 4
    env <- environment()
    ## Correlation between replicates
    chList <- myCall(corrFun, env)
    ## Histograms of replicate and channel intensities
    chList <- myCall(histFun, env)
    ## Plate plot of standard deviations across replicates 
    chList <- myCall(sdFun, env)
    ## Plate plot of replicate and channel intensities
    chList <- myCall(intensFun, env)
    ## Correlation between channels
    chList <- myCall(chanCorrFun, env)
    names(chList) <- channelNames
    stack <- chtsImageStack(chList, id="perExpQC", title=paste("Experiment report for", name),
                            tooltips=addTooltip(names(chList[[1]]), "Help"))
    writeHtml(stack, con=con, vertical=FALSE)
    return(list(url=fn, qmsummary=qmsummary)) 
}



myCall <- function(fun, env)
{
    environment(fun) <- env
    fun()
}

## The scatterplots or image plots for between replicate correlation. Note that the function only works
## in the scope of QMbyPlate since no formal arguments are defined. Instead, all variables are assumed
## to be present in the calling environment. This is also true for all of the following plotting functions.
## In order to make this work, the functions needs to be called through myCall.
## The object  chList holds the lists of chtsImage objects for each channel, and new modules will simply be
## appended
corrFun <- function()
{
    for (ch in 1:nrChannel)
    {
        imgList <- list()
        nrRep <- nrRepCh[ch]
        img <- sprintf("scp_Channel%d.png", ch)
        if(nrRep==2) 
        {
            title <- "Scatterplot between replicates"
            caption <- sprintf("Spearman rank correlation: %s<br>%s",
                               qmsummary[[sprintf("Channel %d", ch)]]["Spearman rank correlation "],
                               wellCount[1,ch])
            makePlot(file.path(basePath, subPath), con=con,
                     name=sprintf("scp_Channel%d", ch), w=plsiz+1, h=plsiz+1, fun = function() 
                 {
                     par(mai=c(0.8,0.8,0.2,0.2), mgp=c(2.5, 1, 0))
                     ylim=c(min(platedat[,,,ch], na.rm=TRUE), max(platedat[,,,ch], na.rm=TRUE))
                     plot(platedat[,,whHasData[[ch]][1],ch], platedat[,,whHasData[[ch]][2],ch],
                          pch=20, cex=0.6, ylim=ylim,
                          xlab=paste("Replicate", whHasData[[ch]][1], sep=" "), 
                          ylab=paste("Replicate", whHasData[[ch]][2], sep=" "), 
                          col=wellTypeColor[mtt[[ch]]]); abline(a=0, b=1, col="lightblue")
                 }
                     , print=FALSE)
            imgList$Correlation <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img), caption=caption,
                                                        additionalCode=sprintf("<div class=\"scatterLegend\">%s</div>",
                                                        wellCount[2,ch])))
        } 
        else if(nrRep>2)
        {
            title <- "Correlation between replicates"
            caption <- NA
            cm <- cor(platedat[,,whHasData[[ch]],ch], method = "spearman", use = "pairwise.complete.obs")
            legend <- seq(0,1,0.2)
            m.legend <- as.matrix(legend)
            MyCol <- colorRampPalette(c("#052947", "white"), 10)
            makePlot(file.path(basePath, subPath), con=con, 
                     name=sprintf("Correlation_ch%d", ch), w=6, h=6, fun = function() 
                 {
                     layout(t(matrix(c(rep(c(3, c(rep(1,10)), 4), 5), 3, rep(2, 10), 4), ncol=6)))
                     image(seq_len(nrow(cm)), seq_len(nrow(cm)), cm, col = "MyCol"(10), 
                           axes = FALSE, zlim=c(0,1), xlab = "", ylab = "")
                     box()
                     axis(side = 1, at=c(1:3), labels = paste("Rep", 1:3)) 
                     axis(side = 2, at=c(1:3), labels = paste("Rep", 1:3))
                     par( mar = c(4, 4, 2, 2))
                     image(m.legend, col = "MyCol"(10), axes = FALSE)
                     box()
                     axis(side = 1, at=seq(0, 1, 0.2))
                 }
                     , print=FALSE)
            imgList$Correlation <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img), caption=caption))
        }
        else 
        {
            imgList$Correlation <- chtsImage(data.frame(caption="No replicates: scatterplot omitted"))
        }
        chList[[ch]] <- c(chList[[ch]], imgList)
    }
    return(chList)
}
    


## The histograms of intensities for the respective replicates and channels.
histFun <- function()
{
    for (ch in 1:nrChannel)
    {
        imgList <- list()
        nrRep <- nrRepCh[ch]
        tabTitle <- sprintf("Histogram%s", ifelse(maxRep>1, "s", ""))
        img <- caption <- title <- NULL
        aa <- c(iwells[["pos"]], iwells[["neg"]], iwells[["act"]], iwells[["inh"]])
        aa <- aa[!is.na(aa)] 
        for (r in 1:maxRep) {
            if (r %in% whHasData[[ch]])
            {
                makePlot(file.path(basePath, subPath), con=con,
                         name=sprintf("hist_Channel%d_%02d",ch,r), w=plsiz, h=plsiz*0.6, fun = function() 
                     {
                         par(mai=c(0.7,0.25,0.01,0.1))
                         hist(platedat[,,r,ch], xlab ="", breaks=brks[[ch]],
                              col = gray(0.95), yaxt = "n", main="")
                         rug(platedat[,,r,ch])
                         for(jj in aa) rug(platedat[,,r,ch][mtt[[ch]]==jj], col=wellTypeColor[jj])
                     }
                         , print=FALSE)
                img <- c(img, sprintf("hist_Channel%d_%02d.png",ch,r))
                cnam <- paste("Dynamic range ", ifelse(maxRep>1, sprintf("(replicate %d)", r), ""))
                dynRange <- as.vector(qmsummary[[sprintf("Channel %d", ch)]][cnam])
                caption <- c(caption, ifelse(is.na(dynRange), "", sprintf("Dynamic range: %s", dynRange)))
                title <- c(title, sprintf("Replicate %d", r))
            } 
            else 
            {
                caption <- c(caption, sprintf("Replicate %d is missing", r))
                img <- c(img, NA)
                title <- c(title, NA)
            }			
        }## for r
        imgList[[tabTitle]] <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                                 fullImage=gsub("png$", "pdf", img), caption=caption))
        chList[[ch]] <- c(chList[[ch]], imgList)      
    } ## for channel
    return(chList)
}



## Plate plot of standard deviations between replicates
sdFun <- function()
{
    if(is.list(plotPlateArgs)) 
    {
        title <- "Standard deviation across replicates"
        for(ch in 1:nrChannel) 
        {
            imgList <- list()
            char <- character(dim(platedat)[1])
            char[pneg[[ch]]] <- "N"
            if (isTwoWay) 
            {
                char[pact[[ch]]] <- "A"
                char[pinh[[ch]]] <- "I"
            } 
            else 
            {
                char[unlist(ppos[[ch]])] <- "P" 
            }
            caption <- sprintf("Repeatability standard deviation: %s",
                               qmsummary[[sprintf("Channel %d", ch)]]["Repeatability standard deviation"])
            img <- sprintf("ppsd_Channel%d.png",ch)
            sdWithNA <- function(x) 
            {
                x <- x[!is.na(x)]
                if(length(x)>0L) sd(x) 
                else as.numeric(NA)
            }
            psd <- apply(platedat[,,,ch,drop=FALSE], 1, sdWithNA)
            if(!all(is.na(psd)))
            {
                if(is.null(plotPlateArgs$sdrange[[ch]]))
                    plotPlateArgs$sdrange[[ch]]=c(0, quantile(psd, 0.95, na.rm=TRUE))
                pp <- makePlot(file.path(basePath, subPath), con=con,
                               name=sprintf("ppsd_Channel%d",ch), w=plsiz+2, fun = function() 
                           {
                               return(plotPlate(psd, nrow=pdim["nrow"], ncol=pdim["ncol"],
                                                main="Standard Deviations",
                                                na.action="xout",
                                                col=plotPlateArgs$sdcol, char=char,
                                                xrange=plotPlateArgs$sdrange[[ch]]))
                           }
                               , print=FALSE, isPlatePlot=TRUE)
                imap <- if(plotPlateArgs$map) 
                    myImageMap(object=pp$coord, tags=list(title=paste(genAnno, ": sd=", signif(psd,3),
                                                          sep="")), img) else ""
                imgList$Reproducibility <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                                                fullImage=gsub("png$", "pdf", img),
                                                                caption=caption, map=imap))
            } 
            else 
            {
                imgList$Reproducibility <-
                    chtsImage(data.frame(caption="No replicates: plate plot omitted"))
            }
            chList[[ch]] <- c(chList[[ch]], imgList)  
        }## for(ch
    }#if(is.list
    return(chList)    
}



## plate plot of intensities for all replicates and channels
intensFun <- function()
{
    if(is.list(plotPlateArgs)) 
    {
        for(ch in 1:nrChannel) 
        {
            imgList <- list()
            char <- character(dim(platedat)[1])
            char[pneg[[ch]]] <- "N"
            if (isTwoWay) 
            {
                char[pact[[ch]]] <- "A"
                char[pinh[[ch]]] <- "I"
            } 
            else 
            {
                char[unlist(ppos[[ch]])] <- "P" 
            }
            title <- caption <- img <- NULL
            for (r in 1:maxRep) 
            {
                if (r %in% whHasData[[ch]])
                {
                    title <- c(title, paste("Replicate ",r))
                    if(is.null(plotPlateArgs$xrange[[ch]]))
                        plotPlateArgs$xrange[[ch]] <- quantile(platedat[,,,ch], c(0.025, 0.975), na.rm=TRUE)
                    pp <- makePlot(file.path(basePath, subPath), con=con,
                                   name=sprintf("pp_Channel%d_%d",ch,r), w=plsiz+1, h=(plsiz+1)*0.66,
                                   fun=function() 
                               {
                                   plotPlate(platedat[,,r,ch], nrow=pdim["nrow"], ncol=pdim["ncol"], 
                                             na.action="xout",
                                             main="Intensities",
                                             col=plotPlateArgs$xcol, char=char,
                                             xrange=plotPlateArgs$xrange[[ch]])
                               }
                                   , print=FALSE, isPlatePlot=TRUE)
                    imap <- if(plotPlateArgs$map) 
                        myImageMap(object=pp$coord, tags=list(title=paste(genAnno, 
                                                              ": value=", signif(platedat[,,r,ch],3), sep="")), 
                                   sprintf("pp_Channel%d_%d.png", ch, r)) else ""
                    img <- c(img, sprintf("pp_Channel%d_%d.png",ch,r))
                    caption <- c(caption, NA)
                } 
                else 
                {## if r %in$ whHasData[[ch]]
                    caption <- c(caption, paste("Replicate ", r, " is missing"))
                    img <- c(img, NA)
                    title <- c(title, NA)
                }## else if r %in% whHasData[[ch]]
            } # maxRep
            imgList$Intensities <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img), caption=caption,
                                                        map=imap))
            chList[[ch]] <- c(chList[[ch]], imgList)  
        } # channel
    } # if(is.list(plotPlateArgs))
    return(chList)
}




## Scatterplot of intensities between channels (only if there are two channels
chanCorrFun <- function()
{
    if (nrChannel==2) 
    {	
        ## correct the color code for the 2-channel scatter plot
        ## For the original configuration plate corrected by the screen log information:
        wellCount <- data.frame(matrix(NA, ncol = maxRep, nrow = 2))
        names(wellCount) = sprintf("Replicate %d", 1:maxRep)
        mtt <- vector("list", length = maxRep)
        ctrls <- if(isTwoWay) unique(c(unlist(pact), unlist(pinh), unlist(pneg))) else unique(c(unlist(ppos),
                                                                                                unlist(pneg)))
        iPNAI <- which(names(wellTypeColor) %in% c("pos", "neg", "act", "inh"))
        for (r in 1:maxRep) 
        {
            mtt[[r]] <- mt
            mtrep <- apply(finalWellAnno[,,r,, drop=FALSE], 4, match, names(wellTypeColor))
            ## set the controls in any of the channels as "controls":
            mtrep[ctrls,] [which(is.na(mtrep[ctrls,]) | mtrep[ctrls,] %in% iPNAI)] <- iwells[["controls"]]
            aa <- apply(finalWellAnno[,,r,, drop=FALSE], 4, function(u) sum(u=="flagged"))
            aa <- order(aa, decreasing=TRUE)
            nrWellTypes <- sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))
            wellCount[1,r] <- if(nrWellTypes[iwells[["flagged"]]])
                paste(sprintf("(%d flagged samples)", nrWellTypes[iwells[["flagged"]]]), collapse=", ") else ""
            wellCount[2, r] <- paste(sprintf("<font color=\"%s\">%s: %d</font>", 
                                             wellTypeColor[-c(iwells[["flagged"]], iPNAI)], 
                                             names(wellTypeColor)[-c(iwells[["flagged"]], iPNAI)], 
                                             nrWellTypes[-c(iwells[["flagged"]], iPNAI)]),
                                     collapse="&nbsp&nbsp&nbsp")
            
            ## so "flagged" or "empty" always wins over "controls" or "sample"
            mtt[[r]][is.na(mtt[[r]])] <- apply(mtrep[is.na(mtt[[r]]),, drop=FALSE], 1, max) 
            
            ## so "controls" always win over "pos" or "neg" or "sample" or "act" or "inh"
            mtt[[r]][!is.na(mtt[[r]])] <- apply(mtrep[!is.na(mtt[[r]]),, drop=FALSE], 1, max) 
            
        }## for r
        
        ## plot title
        tabTitle <- "Channel Correlation"
        title <- img <- caption <- addCode <- NULL
        for (r in 1:maxRep)
        {			
            if((r %in% whHasData[[1]]) & (r %in% whHasData[[2]])){
                ## scatterplot between channels
                makePlot(file.path(basePath, subPath), con=con,
                         name=sprintf("scp_Rep%d", r), w=plsiz, h=plsiz, fun = function()
                     {
                         par(mai=c(0.5,0.5,0.1,0.1))
                         ylim=c(min(platedat, na.rm=TRUE), max(platedat, na.rm=TRUE))
                         plot(platedat[,,r,1], platedat[,,r,2], pch=16, cex=0.5,
                              ylim=ylim, xlab="Channel 1", ylab="Channel 2", col=wellTypeColor[mtt[[r]]])
                         abline(a=0, b=1, col="lightblue")
                     }, print=FALSE)
                img <- c(img, sprintf("scp_Rep%d.png", r))
                title <- c(title, sprintf("Replicate %s", r))
                addCode <- c(addCode, sprintf("<div class=\"scatterLegend\">%s</div>", wellCount[2,r]))
                caption <- c(caption, wellCount[1,r])
            }
            else
            {
                title <- c(title, NA)
                img <- c(img, NA)
                addCode <- c(addCode, NA)
                caption <- c(caption, paste("Replicate ", r, " is missing in one of the channels: ",
                                            "scatterplot omitted", sep=""))
            }## if r
        }## for r
        img <- chtsImage(data.frame(title=title, shortTitle=title, thumbnail=img,
                                    fullImage=gsub("png$", "pdf", img), caption=caption,
                                    additionalCode=addCode))
        for(i in seq_len(nrChannel))
            chList[[i]] <- c(chList[[i]], "Channel Correlation"=img)
    }## if nrChannel
    return(chList)
 }
