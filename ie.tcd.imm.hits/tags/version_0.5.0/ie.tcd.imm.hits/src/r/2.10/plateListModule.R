## The workhorse function for the 'Plate List' module: this is a matrix of quality metrics
## for the different plates and links to the per plate quality reports. 
writeHtml.plateList <- function(cellHTSList, module, exptab, links, center,
                                outdir, htmldir, configured, con, expOrder, ...)
{
    ## Copy the original intensity files for the compedium and also generate HTML files
    nn <- writeIntensityFiles(outdir=outdir, xr=cellHTSList$raw, htmldir=htmldir)
    ## Now the QC results
    writeHtml.header(con, path="../html")
    links[!is.na(links[, "Filename"]),"Filename"] <- nn
    sel <- !is.na(links[, "Status"])
    links[sel,"Status"] <- file.path("../", links[sel,"Status"])
    links <- links[expOrder,]
    
    writeQCTable(exptab, url=links, con=con, configured=configured,
                 xr=cellHTSList$raw)
    writeHtml.trailer(con)
    return(NULL)
}


## Write raw data files from the cellHTS object back to disk, both as regular
## txt files and as formatted HTML 
writeIntensityFiles <- function(outdir, xr, htmldir)
{
    wh <- which(plateList(xr)$Status=="OK")
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
        html <- gsub("in/", "", paste(strsplit(nm[w], ".", fixed=TRUE)[[1]][1],
                                      "html", sep="."))
        con <- file(file.path(htmldir, html), open="w")
        writeHtml.header(con, path="../html")
	writeLines(sprintf("<p class=\"verbatim\">%s</p>", paste(txt, collapse="<br>")),
                   con)
        writeHtml.trailer(con)
        close(con)
        newFileNames <- c(newFileNames, html)
    }
    return(newFileNames)
}



## Function to autogenerate a matrix of class labels from a data
## frame, where common rows as denoted by nrPlates alternate between
## 'odd' and 'even'
plateListClass <-  function(df, nrPlates=rep(1, nrow(df)),
                            classes=c("odd", "even"))
{
    mclass <- matrix(classes[1], ncol=ncol(df), nrow=nrow(df))
    mclass[rep(seq_along(nrPlates)%%2==0, nrPlates)] <- classes[2]
    colnames(mclass) <- colnames(df)
    return(mclass)
}


## The function creating the HTML table of QC scores including all links
writeQCTable <- function(x, url, configured, xr, con)
{
    ## The glossary. We need to fuzzy match the colnames to the glossary because
    ## stuff can be added in parentheses at the end if multiple controls are present.
    cn <- gsub(" \\(.*", "", colnames(x))
    cn[grep("Spearman rank correlation (min - max)", colnames(x), fixed=TRUE)] <-
        "Spearman rank correlation (min - max)"
    glossary <- parseGlossaryXML()
    noFuzzy <- cn %in% c("Plate", "Replicate", "Channel", "Filename", "Status")    
    cn[noFuzzy] <- addTooltip(cn[noFuzzy], fullTag=TRUE, glossary=glossary)
    cn[!noFuzzy] <- addTooltip(cn[!noFuzzy], fullTag=TRUE, glossary=glossary, fuzzy=TRUE)
    
    ## Finding the redundant plates
    ## hwriter does not allow for rowspans, so we have to infuse this into our table
    red <- table(x$Plate)
    stat <- split(url[,"Status"], rep(seq_along(red), as.integer(red)))
    stat <- sapply(stat, function(z) if(all(is.na(z))) NA else unique(z[!is.na(z)]))
    infuse <- sapply(seq_along(red), function(i)
                 {
                     if(is.na(stat[i]))
                         sprintf(paste("</a></td><td rowspan=\"%s\" class=\"detailsEmpty\"",
                                   ">&nbsp&nbsp&nbsp</td><a>"), red[i])
                     else
                         sprintf(paste("</a></td><td rowspan=\"%s\" class=\"details\" ",
                                       "onClick=\"linkToFile('%s')\"%s>&nbsp&nbsp&nbsp</td><a>"),
                                 red[i], stat[i],
                                 addTooltip(sprintf(paste("Detailed QC information for ",
                                                          "plate %s across all",
                                                          "replicates and channels."), i),
                                            fromGlossary=FALSE))
                 })
    ## We want different CSS classes for the different rows to be flexible
    url <- cbind(NA, url)
    x <- cbind(I(""), x)
    url[,"Status"] <- NA
    em <- xr@plateList$errorMessage
    sel <- !is.na(em)
    url <- rbind(NA, url)
    tabClasses <- rbind("header", cbind(I(""), plateListClass(x[,-1], red)))
    for(tc in intersect(colnames(x), c("Plate", "Replicate", "Channel", "Filename", "Status")))
        tabClasses[-1,tc] <- paste(tabClasses[-1,tc], tc)
    tabClasses[-c(1, which(sel)+1),"Status"] <-
        paste(tabClasses[-c(1, which(sel)+1), "Status"], "passed")
    tabClasses[1,1] <- ""
    x[!sel, "Status"] <- "&nbsp"
    ## Error messages should be indicated by tooltips
    er <- x[,"Status"] == "ERROR"
    if(any(sel))
    {
        x[sel&er, "Status"] <- "&nbsp&nbsp"
        x[sel, "Status"] <-
            sprintf("<span %s>%s</span>",
                    addTooltip(xr@plateList$errorMessage[sel],
                               title="Error", fromGlossary=FALSE),
                    x[sel, "Status"])
        tabClasses[which(er & sel)+1, "Status"] <-
            paste(tabClasses[which(er & sel)+1, "Status"], "failed")
    }
    fn <- which(colnames(url)=="Filename")
    tabClasses[-1,fn] <- paste(tabClasses[-1,fn], "link")
    x <- rbind(I(c("", cn)), x)
    x[cumsum(red)-red+2, 1] <- paste(infuse, x[cumsum(red)-red+2, 1], sep="")
    x[1,1] <- "</a></td><td><a>"
    ## No need to produce output for empty lines
    empty <- apply(x[-1,], 2, function(z) all(z == "" | is.na(z)))
    if(any(empty))
    {
        x <- x[,!empty]
        tabClasses <- tabClasses[,!empty]
        url <- url[,!empty]
    }
    x$Channel <- c("Channel", channelNames(xr)[as.numeric(x$Channel[2:length(x$Channel)])])
    ## There are no details if the object hasn't been configured, hence no links
    if(!configured)
    {
        x <- x[,-1]
        url <- url[,-1]
        tabClasses <- tabClasses[,-1]
    }
    tabHTML <-  hwrite(x, row.names=FALSE, col.names=FALSE, class=tabClasses,
                       border=0, table.class="rest", link=url)
    writeLines(hwrite(paste(tabHTML, collapse="\n"), table=TRUE, border=0,
                      table.class="plateList", table.align="center"), con)
}



## Create a HTML image map from a matrix of (rectangular)
## coordinates. Note that this will not create the whole img tag, but
## only the map part, which needs to be injected in the img.
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
        paste(paste("<area shape=\"rect\" coords=\"", paste(object[i,], collapse=","),"\"",
                    sep=""),
              paste(" ", paste(names(tags), "=\"",c(tags[["title"]][i], tags[["href"]][i]),
                               "\"", sep=""),
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
                      #plotPlateArgs,
                      brks, finalWellAnno, activators, inhibitors, positives,
                      negatives, isTwoWay, namePos, wellTypeNames, plateDynRange, plateWithData,
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
    
    samples <- which(mt==which(wellTypeNames=="sample"))
	
    ## summary of the quality metrics in 'qm' to be returned by this function:
    qmsummary <- vector("list", length=nrChannel)
    names(qmsummary) <- sprintf("Channel %d", 1:nrChannel)
	
    ## Create table with per-plate quality metrics
    for (ch in 1:nrChannel)
    {
        nrRep <- nrRepCh[ch]				
        ## 1) create summary table from dynamic range:
        d <- length(plateDynRange)*(maxRep + 1)
        qm <- data.frame(metric=I(character(d)), value=NA, comment=I(character(d)))
        for(i in 1:length(plateDynRange))
        {			
            pn <- if(names(plateDynRange)[i]=="pos" & length(plateDynRange)==1) "" else
            sprintf("'%s'",
                                                            names(plateDynRange)[i])
            qm$metric[(i-1)*(maxRep+1)+(1:maxRep)] <-
                I(sprintf("Dynamic range %s (replicate %s)",pn , 1:maxRep))
            qm$metric[(i-1)*(maxRep+1)+(maxRep+1)] <- I(sprintf("Dynamic range %s",pn))
            qm$value[(i-1)*(maxRep+1)+(1:maxRep)] <-
                round(plateDynRange[[i]][1, 1:maxRep, ch],2)
            qm$value[(i-1)*(maxRep+1)+(maxRep+1)] <-
                round(plateDynRange[[i]][1,maxRep+1, ch],2)
            hasNoVal <- is.na(plateDynRange[[i]][1,1:maxRep,ch]) 
            if(any(hasNoVal))
            {
                if(all(is.na(plateDynRange[[i]][1,,ch])))
                {
                    qm$comment[(i-1)*(maxRep+1) + (1:(maxRep+1))] <-
                        I(sprintf("No controls ('%s' and/or 'neg') were found.",
                                  ifelse(names(plateDynRange)[i] %in% c("activators",
                                                                        "inhibitors"),
                                         names(plateDynRange)[i], "pos"))) 
                }
                else
                {					
                    a <- intersect(hasNoVal, whHasData[[ch]]) 
                    if(length(a)) qm$comment[(i-1)*(maxRep+1) + a] <-
                        I("No available values for one of the controls")
                    b <- setdiff(1:maxRep, whHasData[[ch]])
                    if(length(b))
                        qm$comment[(i-1)*(maxRep+1) + b] <-
                            I(paste(paste("Replicate", b, sep=" "), "is missing", sep=" "))
                } # else all(is.na...
            } #  any(hasNoVal)
        } # for i in 1:length(plateDynRange)		
        ## 2. Correlation coefficient (just for samples wells)
        comm <- ""    
        if(nrRep>1) { ## subPath corresponds to the plate number
            cc1 <- round(repMeasures$repStDev[subPath,ch],2)
            cc2 <- if(maxRep==2) round(repMeasures$corrCoef[subPath,ch],2) else
            paste(round(repMeasures$corrCoef.min[subPath,ch],2),
                  round(repMeasures$corrCoef.max[subPath,ch],2), sep=" - ")
        }
        else
        {
            cc1 <- cc2 <- as.numeric(NA)
            comm <- sprintf("%d replicate%s", nrRep, ifelse(nrRep, "", "s"))
        }
        qm <- rbind(qm, data.frame(metric=I("Repeatability standard deviation"),
                                   value=cc1, comment=I(comm)))
        qm <- rbind(qm, data.frame(metric=I(sprintf("Spearman rank correlation %s",
                                   ifelse(maxRep==2,"","(min - max)"))),
                                   value=cc2, comment=I(comm)))		
            
        ## store data in qmsummary 
        qmsummary[[ch]] <- qm$value
        names(qmsummary[[sprintf("Channel %d", ch)]]) <- qm$metric
    } # for ch
        	
    ## ------------------  Color legend for each channel ----------------------------------
    ## For the original configuration plate corrected by the screen log information:
    wellCount <- data.frame(matrix(NA, ncol=nrChannel, nrow=2))
    names(wellCount) <- sprintf("Channel %d", 1:nrChannel)
    mtt <- vector("list", length=nrChannel)
    iwells <- match(c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh"),
                    wellTypeNames)
    names(iwells) <- c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh")
    colmap <- chtsGetSetting("controls")$col[wellTypeNames]
    if(is.null(colmap))
    {
        warning("Unable to find color mappings in the settings.\n",
                "Falling back to defaults.")
        colmap <- c(sample="black", neg="#377EB8", controls="#4DAF4A",
                    other="#984EA3", empty="#FF7F00", flagged="#A65628",
                    act="#E41A1C", inh="#FFFF33", pos="#E41A1C")
    }
	
    if (hasLessCh & nrChannel==1)
    {
        ## The color code must take into account the common entries
        ## between channels and replicates
        mtt[[1]] <- mt
        fwa <- matrix(finalWellAnno, ncol=prod(dim(finalWellAnno)[3:4]))
        mtrep <- apply(fwa, 2, function(u) match(u, wellTypeNames))
        ## include the controls that were not annotated as "neg" or "pos":
        if (isTwoWay)
        {
            mtrep[pact[[1]],] [which(is.na(mtrep[pact[[1]],]))] <- iwells[["act"]]
            mtrep[pinh[[1]],] [which(is.na(mtrep[pinh[[1]],]))] <- iwells[["inh"]]
        }
        else
        {
            mtrep[unlist(ppos[[1]]),] [which(is.na(mtrep[unlist(ppos[[1]]),]))] <-
                iwells[["pos"]]
        }
        mtrep[pneg[[1]],] [which(is.na(mtrep[pneg[[1]],]))] <- iwells[["neg"]]
		
        ## replace the remaining NA positions by "other" (these corresponds to wells that
        ## although annotated as controls in the configuration file, don't behave as
        ## controls in the current channel
        mtrep[which(is.na(mtrep))] <- iwells[["other"]]
        aa <- apply(fwa, 2, function(u) sum(u=="flagged"))
        aa <- order(aa, decreasing=TRUE) # position 1 contains the replicate with more flagged values
        nrWellTypes <- sapply(seq(along=wellTypeNames), function(i)
                              sum(mtrep[,aa[1]]==i, na.rm=TRUE))
		
        ## flagged wells
        wellCount[1,1] <- paste(sprintf("flagged: %d", nrWellTypes[iwells[["flagged"]]]),
                                collapse=", ")
        ## all the other wells, except controls
        fontColor <- colmap[-c(iwells[["flagged"]],iwells[["controls"]])]
        nbr <- nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]
        fontColor <- fontColor[nbr>0]
        nbr <- nbr[nbr>0]
        wellCount[2, 1] <- paste(sprintf("<font color=\"%s\">%s: %d</font>", fontColor,
                                         names(fontColor), nbr),
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
            mtrep <- apply(finalWellAnno[,,,ch, drop=FALSE], 3, match, wellTypeNames)
            ## include the controls that were not annotated as "neg" or "pos":
            ## correct 'pos' controls just for one-way assays
            if (!isTwoWay)
            {
                if (length(unlist(ppos[[ch]])))
                {
                    mtrep[unlist(ppos[[ch]]),][which(is.na(mtrep[unlist(ppos[[ch]]),]))] <-
                        iwells[["pos"]]
                }
                else
                { ## if length pos
                    ## replace possible wells annotated as "pos" by NA, because they shouldn't
                    ## be considered as a positive control for this channel:
                    if(any(mtt[[ch]] %in% iwells[["pos"]]) && nrChannel>1)
                    {
                        mtrep[mtt[[ch]] %in% iwells[["pos"]],] <- NA
                        mtt[[ch]][mtt[[ch]] %in% iwells[["pos"]]] <- NA 
                    } ## if any
                } ## else length pos
            }
            else
            { ## if !isTwoWay
				
                ## include the controls that were not annotated as
                ## "act" or "neg", but only if they should be regarded
                ## as such in this channel
                if (length(pact[[ch]]))
                {
                    mtrep[pact[[ch]],][which(is.na(mtrep[pact[[ch]],]))] <- iwells[["act"]]
                }
                else
                {## if length act
                    if(any(mtt[[ch]] %in% iwells[["act"]]) && nrChannel>1)
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
                    if(any(mtt[[ch]] %in% iwells[["inh"]]) && nrChannel>1)
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
                if (any(mtt[[ch]] %in% iwells[["neg"]]) && nrChannel>1)
                {
                    mtrep[mtt[[ch]] %in% iwells[["neg"]],] <- NA
                    mtt[[ch]][mtt[[ch]] %in% iwells[["neg"]]] <- NA 
                } ## if any
            } ## else length neg
            
            ## replace the remaining NA positions by "other" (these
            ## corresponds to wells that although annotated as
            ## controls in the configuration file, don't behave as
            ## controls in the current channel
            mtrep[which(is.na(mtrep))] <- iwells[["other"]]
            aa <- apply(finalWellAnno[,,,ch, drop=FALSE], 3, function(u) sum(u=="flagged"))
            aa <- order(aa, decreasing=TRUE)
            nrWellTypes <- sapply(seq(along=wellTypeNames), function(i) sum(mtrep[,aa[1]]==i,
                                      na.rm=TRUE))
			
            ## flagged wells
            wellCount[1,ch] <- if(!is.na(nrWellTypes[iwells[["flagged"]]]))
                paste(sprintf("(%d flagged samples)", nrWellTypes[iwells[["flagged"]]]),
                      collapse=", ") else ""
            ## the other wells, except controls
            fontColor <- colmap[-c(iwells[["flagged"]], iwells[["controls"]])]
            nbr <- nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]
            fontColor <- fontColor[nbr>0]
            nbr <- nbr[nbr>0]
            wellCount[2, ch] <- paste(sprintf("<font color=\"%s\">%s: %d</font>",
                                              fontColor, names(fontColor), nbr),
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
    chList <- myCall(corrFun, env, list(nrChannel=nrChannel, nrRepCh=nrRepCh,
                                        wellCount=wellCount, qmsummary=qmsummary,
                                        wellTypeColor=fontColor,
                                        basePath=basePath, subPath=subPath,
                                        platedat=platedat, whHasData=whHasData,
                                        mtt=mtt))
    ## MA-plot between replicates
    chList <- myCall(maFun, env, list(nrChannel=nrChannel, nrRepCh=nrRepCh,
                                      wellTypeColor=fontColor,
                                      basePath=basePath, subPath=subPath,
                                      platedat=platedat, whHasData=whHasData,
                                      wellCount=wellCount, mtt=mtt))
    
                     
    ## Histograms of replicate and channel intensities
    chList <- myCall(histFun, env,list(nrChannel=nrChannel, nrRepCh=nrRepCh,
                                       maxRep=maxRep, iwells=iwells, brks=brks,
                                       basePath=basePath, subPath=subPath,
                                       platedat=platedat, whHasData=whHasData,
                                       wellTypeColor=fontColor, mtt=mtt,
                                       qmsummary=qmsummary, wellCount=wellCount))
    ## Plate plot of standard deviations across replicates 
    chList <- myCall(sdFun, env, list(nrChannel=nrChannel, platedat=platedat,
                                      pneg=pneg, ppos=ppos, isTwoWay=isTwoWay,
                                      pact=pact, pinh=pinh, qmsummary=qmsummary,
                                      pdim=pdim, basePath=basePath, subPath=subPath,
                                      genAnno=genAnno))
    ## Plate plot of replicate and channel intensities
    chList <- myCall(intensFun, env, list(nrChannel=nrChannel, platedat=platedat,
                                          pneg=pneg, ppos=ppos, isTwoWay=isTwoWay,
                                          pact=pact, pinh=pinh, maxRep=maxRep,
                                          qmsummary=qmsummary, whHasData=whHasData,
                                          pdim=pdim, basePath=basePath, subPath=subPath,
                                          genAnno=genAnno))
    ## Correlation between channels
    chList <- myCall(chanCorrFun, env, list(nrChannel=nrChannel, maxRep=maxRep,
                                            pneg=pneg, ppos=ppos, isTwoWay=isTwoWay,
                                            pact=pact, pinh=pinh, wellTypeColor=fontColor,
                                            mt=mt, finalWellAnno=finalWellAnno,
                                            iwells=iwells, whHasData=whHasData,
                                            plsiz=plsiz, basePath=basePath, subPath=subPath,
                                            platedat=platedat))
    names(chList) <- channelNames
    stack <- chtsImageStack(chList, id="perExpQC", title=paste("Experiment report for", name),
                            tooltips=addTooltip(names(chList[[1]]), ""))
    writeHtml(stack, con=con, vertical=FALSE)
    return(list(url=fn, qmsummary=qmsummary)) 
}



myCall <- function(fun, env, args=list())
{
    environment(fun) <- env
    do.call(fun, args)
}

## The scatterplots or image plots for between replicate
## correlation. Note that the function only works in the scope of
## QMbyPlate if not all formal arguments are defined, but rather
## are assumed to be present in the calling
## environment. This is also true for all of the following plotting
## functions.  In order to make this work, the functions needs to be
## called through myCall.  The object chList holds the lists of
## chtsImage objects for each channel, and new modules will simply be
## appended.
corrFun <- function(nrChannel, nrRepCh, qmsummary, wellCount, basePath,
                    subPath, platedat, whHasData, wellTypeColor, mtt)
{
    for (ch in 1:nrChannel)
    {
        imgList <- list()
        nrRep <- nrRepCh[ch]
        if(nrRep==2) 
        {
            img <- sprintf("scp_Channel%d.png", ch)
            title <- "Scatterplot between replicates"
            whc <- grep("Spearman rank correlation", names(qmsummary[[sprintf("Channel %d", ch)]]))
            caption <- sprintf("Spearman rank correlation: %s<br>%s",
                               qmsummary[[sprintf("Channel %d", ch)]][whc],
                               wellCount[1,ch])
            settings <- chtsGetSetting(c("plateList", "correlation"))
            makePlot(file.path(basePath, subPath),
                     name=sprintf("scp_Channel%d", ch), w=settings$size, h=settings$size,
                     font=settings$font, thumbFactor=settings$thumbFactor,
                     psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                     pdfArgs=list(main="Correlation between replicates"),
                     fun=function(main="", ...)
                 {
                     par(mai=c(0.8,0.8,0.2,0.2), mgp=c(2.5, 1, 0))
                     ylim <- range(platedat[,,,ch], na.rm=TRUE, finite=TRUE)
                     plot(platedat[,,whHasData[[ch]][1],ch], platedat[,,whHasData[[ch]][2],ch],
                          pch=20, cex=0.6, ylim=ylim, main=main,
                          xlab=paste("Replicate", whHasData[[ch]][1], sep=" "), 
                          ylab=paste("Replicate", whHasData[[ch]][2], sep=" "), 
                          col=wellTypeColor[mtt[[ch]]])
                     abline(a=0, b=1, col="lightblue")
                 })
            imgList$Correlation <- chtsImage(data.frame(title=title, shortTitle=title,
                                                        thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img),
                                                        caption=caption,
                                                        additionalCode=sprintf("<div class=\"scatterLegend\">%s</div>",
                                                        wellCount[2,ch])))
        } 
        else if(nrRep>2)
        {
            title <- "Correlation between replicates"
            img <- sprintf("Correlation_ch%d.png", ch)
            caption <- NA
            cm <- cor(platedat[,,whHasData[[ch]],ch], method="spearman",
                      use="pairwise.complete.obs")
            legend <- seq(0,1,0.2)
            m.legend <- as.matrix(legend)
            MyCol <- colorRampPalette(c("#052947", "#E3E7EA"), 10)
            settings <- chtsGetSetting(c("plateList", "correlation"))
            makePlot(file.path(basePath, subPath), name=sprintf("Correlation_ch%d", ch),
                     w=settings$size, h=settings$size,
                     font=settings$font, thumbFactor=settings$thumbFactor,
                     psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                     pdfArgs=list(main="Correlation between replicates"),
                     fun=function(main="", ...) 
                 {
                     layout(t(matrix(c(rep(c(3, c(rep(1,10)), 4), 5), 3, rep(2, 10), 4),
                                     ncol=6)))
                     image(seq_len(nrow(cm)), seq_len(nrow(cm)), cm, col=MyCol(10), 
                           axes=FALSE, zlim=c(0,1), xlab="", ylab="", main=main)
                     box()
                     axis(side=1, at=c(1:3), labels=paste("Rep", 1:3)) 
                     axis(side=2, at=c(1:3), labels=paste("Rep", 1:3))
                     par(mar=c(4, 4, 2, 2))
                     image(m.legend, col=MyCol(10), axes=FALSE)
                     box()
                     axis(side=1, at=seq(0, 1, 0.2))
                 })
            imgList$Correlation <- chtsImage(data.frame(title=title, shortTitle=title,
                                                        thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img),
                                                        caption=caption))
        }
        else 
        {
            imgList$Correlation <-
                chtsImage(data.frame(caption="No replicates: scatterplot omitted"))
        }
        chList[[ch]] <- c(chList[[ch]], imgList)
    }
    return(chList)
}
    


## The histograms of intensities for the respective replicates and channels.
histFun <- function(nrChannel, nrRepCh, maxRep, iwells, basePath,
                    subPath, platedat, whHasData, wellTypeColor, mtt,
                    brks, qmsummary, wellCount)
{
    for (ch in 1:nrChannel)
    {
        imgList <- list()
        nrRep <- nrRepCh[ch]
        tabTitle <- sprintf("Histogram%s", ifelse(maxRep>1, "s", ""))
        img <- caption <- title <- addCode <- NULL
        settings <- chtsGetSetting(c("plateList", "histograms"))
        wcols <- iwells[setdiff(names(wellTypeColor), "sample")]
        histfun <- function(main="", ...)
        {
            par(mai=c(0.7,0.25,0.3,0.1))
            hist(platedat[,,r,ch], xlab ="", breaks=brks[[ch]],
                 col=gray(0.95), yaxt="n", main=main)
            rug(platedat[,,r,ch])
            for(type in names(wcols)) rug(platedat[mtt[[ch]]==wcols[type],,r,ch],
                                          col=wellTypeColor[type], lwd=2)
        }
        densfun <- function(main="", ...)
        {
            par(mai=c(0.7,0.1,0.4,0.1))
            plot(density(platedat[,,r,ch], na.rm=TRUE), axes=FALSE,
                 xlab="", ylab="", main=main)
            axis(1)
            abline(h=par("usr")[3])
            for(type in names(wcols)){
                xval <- platedat[mtt[[ch]]==wcols[type],,r,ch] 
                points(xval, rep(par("usr")[3]/2, length(xval)),
                                 col=wellTypeColor[type], pch=20)
            }
        }
        fun <- switch(tolower(settings$type),
                      "histogram"=histfun,
                      "density"=densfun,
                  {warning("Unknown plot type. Falling back to default (histogram).")
                   histfun})
        for (r in 1:maxRep) {
            if (r %in% whHasData[[ch]])
            {
               
                makePlot(file.path(basePath, subPath),
                         name=sprintf("hist_Channel%d_%02d",ch,r),
                         w=settings$size, h=settings$size*0.6,
                         font=settings$font, thumbFactor=settings$thumbFactor,
                         psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                         pdfArgs=list(main=sprintf("Distribution Channel %d Replicate %d",
                                      ch, r)),
                     fun=fun)
                img <- c(img, sprintf("hist_Channel%d_%02d.png",ch,r))
                cnam <- paste("Dynamic range ", ifelse(maxRep>1,
                                                       sprintf("(replicate %d)", r), ""))
                dynRange <- as.vector(qmsummary[[sprintf("Channel %d", ch)]][cnam])
                caption <- c(caption, ifelse(is.na(dynRange), "",
                                             sprintf("Dynamic range: %s", dynRange)))
                title <- c(title, sprintf("Replicate %d", r))
                addCode <- c(addCode, sprintf("<div class=\"scatterLegend\">%s</div>",
                                              wellCount[2,ch]))
            } 
            else 
            {
                caption <- c(caption, sprintf("Replicate %d is missing", r))
                img <- c(img, NA)
                title <- c(title, NA)
                addCode <- c(addCode, NA)
            }			
        }## for r
        imgList[[tabTitle]] <- chtsImage(data.frame(title=title, shortTitle=title,
                                                    thumbnail=img,
                                                    fullImage=gsub("png$", "pdf", img),
                                                    caption=caption,
                                                    additionalCode=addCode))
        chList[[ch]] <- c(chList[[ch]], imgList)      
    } ## for channel
    return(chList)
}



## Plate plot of standard deviations between replicates
sdFun <- function(nrChannel, platedat, pneg, ppos, isTwoWay, pact, pinh, qmsummary,
                  pdim, basePath, subPath, genAnno)
{
    ## Load the settings for the reproducibility plate plots and only
    ## produce them if include==TRUE
    settingsA <- chtsGetSetting(c("plateList", "reproducibility"))
    settingsB <- chtsGetSetting(c("plateList", "average"))
    if(settingsA$include) 
    {
        statWithNA <- function(x, fun=sd) 
        {
            x <- x[!is.na(x)]
            if(length(x)>0L) fun(x) 
            else as.numeric(NA)
        }
        titleA <- "Standard deviation across replicates"
        titleB <- "Mean replicate values"
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
            imgA <- sprintf("ppsd_Channel%d.png",ch)
            imgB <- sprintf("ppmean_Channel%d.png",ch)
           
            psd <- apply(platedat[,,,ch,drop=FALSE], 1, statWithNA)
            msd <- apply(platedat[,,,ch,drop=FALSE], 1, statWithNA, mean)
            if(!all(is.na(psd)))
            {
                odim <- optimalDevDims(pdim["nrow"], pdim["ncol"],
                                       settingsA$size, settingsA$size*0.66)
                ## The standard deviation plot first
                ppA <- makePlot(file.path(basePath, subPath),
                                name=sprintf("ppsd_Channel%d",ch),
                                w=odim[1], h=odim[2],
                                font=settingsA$font, thumbFactor=settingsA$thumbFactor,
                                psz=settingsA$fontSize, thumbPsz=settingsA$thumbFontSize,
                                pdfArgs=list(main="Standard deviations"),
                                fun=function(main="", ...) 
                            {
                                return(plotPlate(psd, nrow=pdim["nrow"], ncol=pdim["ncol"],
                                                 main=main,
                                                 na.action="xout",
                                                 col=settingsA$col, char=char,
                                                 xrange=settingsA$range))
                            })
                imapA <- if(settingsA$map) 
                    myImageMap(object=ppA$coord, tags=list(title=paste(genAnno, ": sd=",
                                                                      signif(psd, 3),
                                                                      sep="")), imgA) else ""
                imgList$Reproducibility <- chtsImage(data.frame(title=titleA, shortTitle=titleA,
                                                                thumbnail=imgA,
                                                                fullImage=gsub("png$", "pdf",
                                                                imgB),
                                                                caption=caption, map=imapA))
                ## Now the mean values
                if(settingsB$include) 
                {
                    ppB <- makePlot(file.path(basePath, subPath),
                                    name=sprintf("ppmean_Channel%d",ch),
                                    w=odim[1], h=odim[2],
                                    font=settingsB$font, thumbFactor=settingsB$thumbFactor,
                                    psz=settingsB$fontSize, thumbPsz=settingsB$thumbFontSize,
                                    pdfArgs=list(main="Mean values"),
                                    fun=function(main="", ...) 
                                {
                                    return(plotPlate(msd, nrow=pdim["nrow"], ncol=pdim["ncol"],
                                                     main=main,
                                                     na.action="xout",
                                                     col=settingsB$col, char=char,
                                                     xrange=settingsB$range))
                                })
                    imapB <- if(settingsB$map) 
                        myImageMap(object=ppB$coord, tags=list(title=paste(genAnno, ": mean=",
                                                                           signif(msd, 3),
                                                                           sep="")), imgB) else ""
                    imgList$Average <- chtsImage(data.frame(title=titleB, shortTitle=titleB,
                                                            thumbnail=imgB,
                                                            fullImage=gsub("png$", "pdf",
                                                                           imgB), map=imapB,
                                                            caption=paste("Plate mean:",
                                                                          signif(mean(msd, na.rm=TRUE), 3))))
                }
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
intensFun <- function(nrChannel, platedat, pneg, ppos, isTwoWay, pact, pinh, maxRep,
                      qmsummary, whHasData, pdim, basePath, subPath, genAnno)
{
    ## Load the settings for the intensity plate plots and only
    ## produce them if include==TRUE
    settings <- chtsGetSetting(c("plateList", "intensities"))
    if(settings$include) 
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
            title <- caption <- img <- imap <- NULL
            for (r in 1:maxRep) 
            {
                if (r %in% whHasData[[ch]])
                {
                    title <- c(title, paste("Replicate ",r))
                    odim <- optimalDevDims(pdim["nrow"], pdim["ncol"],
                                           settings$size, settings$size*0.66)
                    pp <- makePlot(file.path(basePath, subPath),
                                   name=sprintf("pp_Channel%d_%d",ch,r),
                                   w=odim[1], h=odim[2],
                                   font=settings$font, thumbFactor=settings$thumbFactor,
                                   psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                                   pdfArgs=list(main=sprintf("Intensities Channel %s Replicate %s",
                                                ch,r)),
                                   fun=function(main="", ...) 
                               {
                                   plotPlate(platedat[,,r,ch], nrow=pdim["nrow"],
                                             ncol=pdim["ncol"], 
                                             na.action="xout",
                                             main=main,
                                             col=settings$col, char=char,
                                             xrange=settings$range)
                               })
                    imap <- c(imap, if(settings$map)
                              myImageMap(object=pp$coord, tags=list(title=paste(genAnno, 
                                                                    ": value=",
                                                                    round(platedat[,,r,ch],3),
                                                                    sep="")), 
                                         sprintf("pp_Channel%d_%d.png", ch, r)) else "")
                    img <- c(img, sprintf("pp_Channel%d_%d.png",ch,r))
                    caption <- c(caption, NA)
                } 
                else 
                {## if r %in$ whHasData[[ch]]
                    caption <- c(caption, paste("Replicate ", r, " is missing"))
                    img <- c(img, NA)
                    title <- c(title, NA)
                    imap <- c(imap, NA)
                }## else if r %in% whHasData[[ch]]
            } # maxRep
            imgList$Intensities <- chtsImage(data.frame(title=title, shortTitle=title,
                                                        thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img),
                                                        caption=caption,
                                                        map=imap))
            chList[[ch]] <- c(chList[[ch]], imgList)  
        } # channel
    } # if(is.list(plotPlateArgs))
    return(chList)
}




## Scatterplot of intensities between channels (only if there are two channels
chanCorrFun <- function(nrChannel, maxRep, isTwoWay, pact, pinh, pneg, ppos,
                        wellTypeColor, mt, finalWellAnno, iwells, whHasData,
                        basePath, subPath, plsiz, platedat)
{
    if (nrChannel==2) 
    {	
        ## correct the color code for the 2-channel scatter plot
        ## For the original configuration plate corrected by the screen log information:
        wellCount <- data.frame(matrix(NA, ncol=maxRep, nrow=2))
        names(wellCount) <- sprintf("Replicate %d", 1:maxRep)
        mtt <- vector("list", length=maxRep)
        ctrls <- if(isTwoWay) unique(c(unlist(pact), unlist(pinh), unlist(pneg))) else
        unique(c(unlist(ppos), unlist(pneg)))
        iPNAI <- which(names(wellTypeColor) %in% c("pos", "neg", "act", "inh"))
        for (r in 1:maxRep) 
        {
            mtt[[r]] <- mt
            mtrep <- apply(finalWellAnno[,,r,, drop=FALSE], 4, match, names(wellTypeColor))
            ## set the controls in any of the channels as "controls":
            mtrep[ctrls,] [which(is.na(mtrep[ctrls,]) | mtrep[ctrls,] %in% iPNAI)] <-
                iwells[["controls"]]
            aa <- apply(finalWellAnno[,,r,, drop=FALSE], 4, function(u) sum(u=="flagged"))
            aa <- order(aa, decreasing=TRUE)
            nrWellTypes <- sapply(seq(along=wellTypeColor), function(i)
                                  sum(mtrep[,aa[1]]==i, na.rm=TRUE))
            wellCount[1,r] <- if(!is.na(nrWellTypes[iwells[["flagged"]]]))
                paste(sprintf("(%d flagged samples)", nrWellTypes[iwells[["flagged"]]]),
                      collapse=", ") else ""
            wellCount[2, r] <- paste(sprintf("<font color=\"%s\">%s: %d</font>", 
                                             wellTypeColor[-c(iwells[["flagged"]], iPNAI)], 
                                             names(wellTypeColor)[-c(iwells[["flagged"]],
                                                                     iPNAI)], 
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
                makePlot(file.path(basePath, subPath),
                         name=sprintf("scp_Rep%d", r), w=plsiz, h=plsiz, fun=function()
                     {
                         par(mai=c(0.5,0.5,0.1,0.1))
                         ylim <- range(platedat, na.rm=TRUE, finite=TRUE)
                         plot(platedat[,,r,1], platedat[,,r,2], pch=16, cex=0.5,
                              ylim=ylim, xlab="Channel 1", ylab="Channel 2",
                              col=wellTypeColor[mtt[[r]]])
                         abline(a=0, b=1, col="lightblue")
                     })
                img <- c(img, sprintf("scp_Rep%d.png", r))
                title <- c(title, sprintf("Replicate %s", r))
                addCode <- c(addCode, sprintf("<div class=\"scatterLegend\">%s</div>",
                                              wellCount[2,r]))
                caption <- c(caption, wellCount[1,r])
            }
            else
            {
                title <- c(title, NA)
                img <- c(img, NA)
                addCode <- c(addCode, NA)
                caption <- c(caption, paste("Replicate ", r,
                                            " is missing in one of the channels: ",
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


## Return the optimal dimensions for the plate plot in inches.
optimalDevDims <- function(nrow, ncol, width, height)
{
    if(ncol>nrow)
    {
        height <- min(height, width/(((ncol+1)*0.1+ncol+1)/((nrow+1)*0.1+nrow+1)))
    }else{
        width <- min(width, height/(((nrow+1)*0.1+nrow+1)/((ncol+1)*0.1+ncol+1)))
    }
    return(c(width=width, height=height))
}






## MA plots of two replicates, or each replicate against the average if
## more than two.
maFun <- function(nrChannel, nrRepCh, basePath, subPath, platedat, whHasData,
                  wellTypeColor, mtt, wellCount)
{
    for (ch in 1:nrChannel)
    {
        imgList <- list()
        nrRep <- nrRepCh[ch]
        settings <- chtsGetSetting(c("plateList", "maplot"))
        if(nrRep==2) 
        {
            average <- rowMeans(platedat[,,,ch], na.rm=TRUE)
            img <- sprintf("map_Channel%d.png", ch)
            title <- "MA-plot across replicates"
            makePlot(file.path(basePath, subPath),
                     name=sprintf("map_Channel%d", ch), w=settings$size, h=settings$size,
                     font=settings$font, thumbFactor=settings$thumbFactor,
                     psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                     pdfArgs=list(main="MA-plot across replicates"),
                     fun=function(main="", ...)
                 {
                     par(mai=c(0.8,0.8,0.2,0.2), mgp=c(2.5, 1, 0))
                     pdr1 <- platedat[,,1,ch]
                     pdr2 <- platedat[,,2,ch]
                     sel <- pdr1>0 & pdr2>0
                     pdr1[!sel] <- pdr2[!sel] <- NA
                     m <- log2(pdr1) - log2(pdr2)
                     a <- 0.5 * (log2(pdr1) + log2(pdr2))
                     plot(m, a, pch=20, cex=0.6, main=main,
                          xlab="M (log-intensity ratio)",
                          ylab="A (log-intensity average)",
                          ylim=c(-1,1) * max(abs(a), na.rm=TRUE),
                          col=wellTypeColor[mtt[[ch]]])
                     abline(h=0, col="lightblue")
                 })
            imgList$"M-A Plot" <- chtsImage(data.frame(title=title, shortTitle=title,
                                                       thumbnail=img,
                                                       fullImage=gsub("png$", "pdf", img),
                                                       additionalCode=sprintf("<div class=\"scatterLegend\">%s</div>",
                                                                              wellCount[2,ch])))
        } 
        else if(nrRep>2)
        {
            average <- rowMeans(platedat[,,,ch], na.rm=TRUE)
            title <- img <- caption <- NULL
            for(r in seq_len(nrRep))
            {
                if (r %in% whHasData[[ch]])
                {
                    title <- c(title, paste("Replicate",r, "vs. average"))
                    makePlot(file.path(basePath, subPath),
                             name=sprintf("map_Channel%d_%d", ch, r), w=settings$size,
                             h=settings$size,
                             font=settings$font, thumbFactor=settings$thumbFactor,
                             psz=settings$fontSize, thumbPsz=settings$thumbFontSize,
                             pdfArgs=list(main="MA-plot against replicate average"),
                             fun=function(main="", ...)
                         {
                             par(mai=c(0.8,0.8,0.2,0.2), mgp=c(2.5, 1, 0))
                             pdr1 <- platedat[,,r,ch]
                             sel <- pdr1>0 & average>0
                             pdr2 <- average
                             pdr1[!sel] <- pdr2[!sel] <- NA
                             m <- log2(pdr1) - log2(pdr2)
                             a <- 0.5 * (log2(pdr1) + log2(pdr2))
                             plot(m, a, pch=20, cex=0.6, main=main,
                                  xlab="M (log-intensity ratio)",
                                  ylab="A (log-intensity average)",
                                  ylim=c(-1,1) * max(abs(a), na.rm=TRUE),
                                  col=wellTypeColor[mtt[[ch]]])
                             abline(h=0, col="lightblue")
                         })
                    img <- c(img, sprintf("map_Channel%d_%d.png",ch,r))
                    caption <- c(caption, NA)
                } 
                else 
                {## if r %in$ whHasData[[ch]]
                    caption <- c(caption, paste("Replicate ", r, " is missing"))
                    img <- c(img, NA)
                    title <- c(title, NA)
                }## else if r %in% whHasData[[ch]]
            } # maxRep
            imgList$"M-A Plot" <- chtsImage(data.frame(title=title, shortTitle=title,
                                                        thumbnail=img,
                                                        fullImage=gsub("png$", "pdf", img),
                                                        caption=caption,
                                                       additionalCode=sprintf("<div class=\"scatterLegend\">%s</div>",
                                                                              wellCount[2,ch])))
        }
        else 
        {
            imgList$"M-A Plot" <-
                chtsImage(data.frame(caption="No replicates: M-A plot omitted"))
        }
        chList[[ch]] <- c(chList[[ch]], imgList)
    }
    return(chList)
}
    
