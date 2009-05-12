# All rights reserved. (C) Copyright 2009, Trinity College Dublin
# Copyright 2008 Michael Boutros and Ligia P. Bras L and Wolfgang Huber

## Correct wellAnno information:
## ... by taking into account the wells that were flagged in the screen log file, 
## or even manually by the user in the assayData slot. Besides the categories in
## wellAnno(x), it contains the category "flagged".
## Returns an array with corrected well annotation.
getArrayCorrectWellAnno <- function(x)
{
    wAnno <- wellAnno(x)
    xx <- Data(x)
    d <- dim(xx)
    correctedWellAnno <- array(rep(wAnno, times = prod(d[2:3])), dim=d)
    ## see which wells are flagged, excluding "empty" wells
    iflagged <- as.logical(is.na(xx)*(wAnno!="empty"))
    correctedWellAnno[iflagged]="flagged"
    return(correctedWellAnno)
}



 defaultColOrder <- function() {
		 return(c("plate",
				 "well",
				 "channelGroupStart",
				 "score",
				 "channelGroupEnd",
				 "wellAnno",
				 "finalWellAnno",
				 "replicateGroupStart",
				 "channelGroupStart",
				 "raw",
				 "channelGroupEnd",
				 "replicateGroupEnd",
				 "channelGroupStart",
				 "median", "diffOrMean",
				 "channelGroupEnd",
				 "replicateGroupStart",
				 "channelGroupStart",
				 "rawPerMedian",
				 "channelGroupEnd",
				 "replicateGroupEnd",
				 "replicateGroupStart",
				 "channelGroupStart",
				 "norm",
				 "channelGroupEnd",
				 "replicateGroupEnd",
				 "geneID",
				 "geneSymbol",
				 "geneAnnotation"
		 ))
	 }


## getTopTable function
## Function to obtain the topTable data.frame and export it as a txt file.

getTopTable <- function(cellHTSlist, file="topTable.txt", verbose=interactive(), channels=channelNames(cellHTSlist[["scored"]]),
		colOrder=defaultColOrder()) {
    ## arguments:
    ## 'cellHTSlist' should be a list of cellHTS object(s) obtained from the same experimental data. Allowed components are:
    ## 'scored' - (mandatory) cellHTS object comprising scored data.
    ## 'raw' - (optional) cellHTS object containing raw experiment data.
    ## 'normalized' - (optional) cellHTS object containing normalized data.
    ## cellHTSlist=list("scored"=xsc, "raw"=xr, "normalized"=xn)

	chGroupStart="channelGroupStart"
	chGroupEnd="channelGroupEnd"
	repGroupStart="replicateGroupStart"
	repGroupEnd="replicateGroupEnd"

    mandatoryComps <- c("scored")
    optionalComps <- c("raw", "normalized") 

    ## A lot of sanity checking up front
    if(!is.list(cellHTSlist))
    {
        stop("Argument 'cellHTSlist' should be a list containing one or a ",
             "maximum of 3 'cellHTS' objects.") 
    }
    else
    {
        if(!all(sapply(cellHTSlist, class)=="cellHTS"))
            stop("Argument 'cellHTSlist' should be a list of cellHTS objects!")
        nm <- names(cellHTSlist)
        if(!(mandatoryComps %in% nm))
            stop("Argument 'cellHTSlist' should be a list containing at least ",
                 "one component named 'scored' that corresponds to a scored ",
                 "'cellHTS' object.")
        if( length(cellHTSlist)>3 | any(duplicated(nm)) )
            stop("Argument 'cellHTSlist' can only have a maximum of 3 components ",
                 "named 'raw', 'normalized' and 'scored'!")
        if(!all(nm %in% c(optionalComps, mandatoryComps))) 
            stop(sprintf("Invalid named component%s in argument 'cellHTSlist': %s", 
                         ifelse(sum(!(nm %in% c(optionalComps, mandatoryComps)))>1, "s", ""), 
                         nm[!(nm %in% c(optionalComps, mandatoryComps))])) 
    }

    xr <- cellHTSlist[["raw"]]
    xn <- cellHTSlist[["normalized"]]
    xsc <- cellHTSlist[["scored"]]
    
    ## now check whether the given components of 'cellHTSlist' are valid cellHTS objects:
    if(!(state(xsc)[["scored"]]))
        stop(sprintf(paste("The component 'scored' of argument list 'cellHTSlist' should be ",
                           "a 'cellHTS' object containing scored data!\nPlease check its ",
                           "preprocessing state: %s"), paste(names(state(xsc)), "=",
                                                             state(xsc), collapse=", ")))
    if(!is.null(xr))
    {
        if(any(state(xr)[c("normalized", "scored")]))
            stop(sprintf(paste("The component 'raw' of argument list 'cellHTSlist' ",
                               "should be a 'cellHTS' object containing raw data!",
                               "\nPlease check its preprocessing state: %s"),
                         paste(names(state(xr)), "=", state(xr), collapse=", ")))
        if(!compare2cellHTS(xsc, xr))
            stop("Difference across 'cellHTS' objects! The scored 'cellHTS' ",
                 "object given in cellHTSlist[['scored']] was not calculated ",
                 "from the data stored in 'cellHTS' object indicated in ",
                 "'cellHTSlist[['raw']]'!")
    }
    if(!is.null(xn))
    {
        if(!(state(xn)[["normalized"]] & !state(xn)[["scored"]]))
            stop(sprintf(paste("The component 'normalized' of argument list 'cellHTSlist' ",
                               "should be a 'cellHTS' object containing normalized data!",
                               "\nPlease check its preprocessing state: %s"),
                         paste(names(state(xn)), "=", state(xn), collapse=", ")))
        if(!compare2cellHTS(xsc, xn))
            stop("'cellHTS' objects contained in cellHTSlist[['scored']] and ",
                 "cellHTSlist[['normalized']] are not from the same experiment!")
    }

    xraw <- if(is.null(xr)) xr else Data(xr)
    xnorm <- if(is.null(xn)) xn else Data(xn)
    scores <- Data(xsc)
    d <- if(is.null(xn)) dim(scores) else dim(xnorm)
    nrWell <- prod(pdim(xsc))
    nrPlate <- max(plate(xsc))
    nrReplicate <- d[2]
    nrChannel <- d[3]
    wAnno <- wellAnno(xsc)
    

    ## array with corrected wellAnno information (by taking into account the wells
    ## that were flagged in the screen log file, or even manually by the user).
    ## Besides the categories in wellAnno(x), it contains the category "flagged".
    scoresWellAnno <- getArrayCorrectWellAnno(xsc)

    ## include also the final well annotation (after the screen log file)
    out <- data.frame(
		firstCol = rep("", length(plate(xsc)))
    )

    #out <- data.frame(
    #		plate=plate(xsc),
    #		position=position(xsc),
    #		well=well(xsc),
    #		score=as.vector(Data(xsc)[, 1, ]), 
    #		wellAnno = wAnno,
    #		finalWellAnno = as.vector(scoresWellAnno[, 1, ])   ## include also the final well annotation (after the screen log file)
    #)

	addCol <- function(res, col, r, ch)
	{
		res <- switch (col,
			plate={res$plate=plate(xsc); res},
			position={res["position"]=position(xsc); res},
			well={res["well"]=well(xsc); res},
			score={
				#out["score"]=as.vector(Data(xsc)[, 1, 1])
				res[sprintf("score_%s", channels[[ch]])] <- round(as.vector(assayData(xsc)[[paste("score", ch, sep="_ch")]]), 3)
				res
			},
			wellAnno={res["wellAnno"] = wAnno; res},
			finalWellAnno={
				if (is.na(ch))
				{
					res["finalWellAnno"] = as.vector(scoresWellAnno[, 1, 1])   ## include also the final well annotation (after the screen log file)
					res
				}
				else
				{
					res[sprintf("finalWellAnno_%s", channels[[ch]])] = as.vector(scoresWellAnno[, 1, ch])   ## include also the final well annotation (after the screen log file)
					res
				}
				res
			},
			median={
				res[sprintf("median_%s", channels[[ch]])] <- apply(xraw[,,ch], 1, median, na.rm=TRUE)
				res
			},
			diffOrMean={
				if(nrReplicate==2) { 
		          ## Difference between replicates (raw data)
		          res[sprintf("diff_%s", channels[[ch]])] = apply(xraw[,,ch], 1, diff)
		        } else {
		          ## average between replicates (raw data)
		          res[sprintf("average_%s", channels[[ch]])] = apply(xraw[,,ch], 1, mean, na.rm=TRUE)
		        } 
				res
			},
			raw={
				res[sprintf("raw_r%d_%s", r, channels[[ch]])] = xraw[,r,ch]
				res
			},
			rawPerMedian={
				## raw/plateMedian
				xrp <- array(as.numeric(NA), dim=dim(xraw))
				isSample <- (as.character(wAnno) == "sample")
				for(p in 1:nrPlate) {
					indp <- (1:nrWell)+nrWell*(p-1)
					samples <- isSample[indp]
					xrp[indp,,] <- apply(xraw[indp,,,drop=FALSE], 2:3, function(j) j/median(j[samples], na.rm=TRUE))
				}
				
				res[sprintf("raw/PlateMedian_r%d_%s", r, channels[[ch]])] <- signif(xrp[,r,ch], 3)
				res
			},
			norm={
				if(!is.null(xnorm)){
					## Include the normalized values
					res[sprintf("normalized_r%d_%s", r, channels[[ch]])] = round(xnorm[,r,ch], 3)
					res
				}
				res
			},
			geneID={
				if(state(xsc)[["annotated"]]) {
					n <- tolower(names(fData(xsc)))
					#res <- cbind(res, fData(xsc)[, !(n %in% tolower(c("controlStatus", "plate", "well", tolower(names(out)))))])
					res["GeneID"] <- fData(xsc)[, "GeneID"]
					res
				}
				res
			},
			geneSymbol={
				if(state(xsc)[["annotated"]]) {
					n <- tolower(names(fData(xsc)))
					res["GeneSymbol"] <- fData(xsc)[, "GeneSymbol"]
					res
				}
				res
			},
			geneAnnotation={
				if(state(xsc)[["annotated"]]) {
					n <- tolower(names(fData(xsc)))
					res <- cbind(res, fData(xsc)[, !(n %in% tolower(c("controlStatus",  "plate", "well", "GeneID", "GeneSymbol", tolower(names(out)))))])
					res
				}
				res
			},
			empty={
				res[sprintf("empty%d", max(0, (0:100)[sprintf("empty%d",0:100) %in% names(res)])+1)] = rep("", length(plate(xsc)))
				res
			},
			stop(sprintf("Invalid value '%s' for argument 'col'", col)))
		res
	}

	colCount = length(colOrder)
	idx = 1:colCount
	processList <- function(res, cols, ch, repl)
	{
		if (length(cols != 0))
		{
			if (cols[1] == chGroupStart)
			{
				end = idx[cols == chGroupEnd][1]
				for (channel in 1: nrChannel)
				{
					res <- processList(res, cols[2:(end-1)], channel, repl)
				}
				if (length(cols) > end)
				{
					res <- processList(res, cols[(end+1):length(cols)], ch, repl)
				}
			}
			else
			if (cols[1] == repGroupStart)
			{
				end = idx[cols == repGroupEnd][1]
				for (replicate in 1: nrReplicate)
				{
					res <- processList(res, cols[2:(end-1)], ch, replicate)
				}
				if (length(cols) > end)
				{
					res <- processList(res, cols[(end+1):length(cols)], ch, repl)
				}
			}
			else
			{
				res <- addCol(res=res, col=cols[1], ch=ch, r=repl)
				if (length(cols)>1)
				{
					res <- processList(res, cols[2:length(cols)], ch, repl)
				}
			}
			res
		}
		res
	}
	out <- processList(out, colOrder, NA, NA)
  
    ## Export everything to the file
    #out <- out[order(out$score, decreasing=TRUE), ]
    #out$score <- round(out$score, 2)
	firstCol = sprintf("score_%s", channels[1])
    if(!is.null(file))
    {
        if (firstCol %in% names(out))
        {
		    out = out[order(out[[firstCol]], decreasing=TRUE), ]
        }
        if (dim(out)[2] > 1)
        {
		    out = out[, 2:(dim(out)[2])]
        }
        if(verbose)
            cat(sprintf("Saving 'topTable' list in file '%s'\n", file))
        write.tabdel(out, file=file)
    }
    return(out)
}
