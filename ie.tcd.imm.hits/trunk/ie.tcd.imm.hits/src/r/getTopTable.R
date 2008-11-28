 ## Correct wellAnno information:
     ## ... by taking into account the wells that were flagged in the screen log file, 
     ## or even manually by the user in the assayData slot. Besides the categories in wellAnno(x), it contains the category "flagged".
## Returns an array with corrected well annotation.
getArrayCorrectWellAnno <- function(x){
  wAnno <- wellAnno(x)
  xx <- Data(x)
  d <- dim(xx)
  correctedWellAnno <- array(rep(wAnno, times = prod(d[2:3])), dim=d)
  ## see which wells are flagged, excluding "empty" wells
  iflagged = as.logical(is.na(xx)*(wAnno!="empty"))
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


## =================================================================================
## getTopTable function
## Function to obtain the topTable data.frame and export it as a txt file.

getTopTable <- function(cellHTSlist, file="topTable.txt", verbose=interactive(), channels=paste("ch_", 1:dim(cellHTSlist[["scored"]])[2], sep=""),
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

if(!is.list(cellHTSlist)) {
   stop("Argument 'cellHTSlist' should be a list containing one or a maximum of 3 'cellHTS' objects.") 
} else {


 if(!all(sapply(cellHTSlist, class)=="cellHTS")) stop("Argument 'cellHTSlist' should be a list of cellHTS objects!")

 nm <- names(cellHTSlist)
 if(!(mandatoryComps %in% nm)) stop("Argument 'cellHTSlist' should be a list containing at least one component named 'scored' that corresponds to a scored 'cellHTS' object.")

 if( length(cellHTSlist)>3 | any(duplicated(nm)) ) stop("Argument 'cellHTSlist' can only have a maximum of 3 components named 'raw', 'normalized' and 'scored'!")

 if(!all(nm %in% c(optionalComps, mandatoryComps))) 
     stop(sprintf("Invalid named component%s in argument 'cellHTSlist': %s", 
         ifelse(sum(!(nm %in% c(optionalComps, mandatoryComps)))>1, "s", ""), 
                       nm[!(nm %in% c(optionalComps, mandatoryComps))])) 
}

xr <- cellHTSlist[["raw"]]
xn <- cellHTSlist[["normalized"]]
xsc <- cellHTSlist[["scored"]]

# now check whether the given components of 'cellHTSlist' are valid cellHTS objects:
  if(!(state(xsc)[["scored"]])) stop(sprintf("The component 'scored' of argument list 'cellHTSlist' should be a 'cellHTS' object containing scored data!\nPlease check its preprocessing state: %s", paste(names(state(xsc)), "=", state(xsc), collapse=", ")))


  if(!is.null(xr)) {
   if(any(state(xr)[c("normalized", "scored")])) stop(sprintf("The component 'raw' of argument list 'cellHTSlist' should be a 'cellHTS' object containing raw data!\nPlease check its preprocessing state: %s", paste(names(state(xr)), "=", state(xr), collapse=", ")))
   if(!compare2cellHTS(xsc, xr)) stop("Difference across 'cellHTS' objects! The scored 'cellHTS' object given in cellHTSlist[['scored']] was not calculated from the data stored in 'cellHTS' object indicated in 'cellHTSlist[['raw']]'!")
  }

   if(!is.null(xn)) {
     if(!(state(xn)[["normalized"]] & !state(xn)[["scored"]])) stop(sprintf("The component 'normalized' of argument list 'cellHTSlist' should be a 'cellHTS' object containing normalized data!\nPlease check its preprocessing state: %s", paste(names(state(xn)), "=", state(xn), collapse=", ")))

   if(!compare2cellHTS(xsc, xn)) stop("'cellHTS' objects contained in cellHTSlist[['scored']] and cellHTSlist[['normalized']] are not from the same experiment!")
  }

## --------------------------------------

    xraw <- if(is.null(xr)) xr else Data(xr)
    xnorm <- if(is.null(xn)) xn else Data(xn)
    scores <- Data(xsc)
    d <- if(is.null(xn)) dim(scores) else dim(xnorm)
    nrWell <- prod(pdim(xsc))
    nrPlate <- max(plate(xsc))
    nrReplicate <- d[2]
    nrChannel <- d[3]

    wAnno <- wellAnno(xsc)


  ## array with corrected wellAnno information (by taking into account the wells that were flagged in the screen log file, or even manually by the user). Besides the categories in wellAnno(x), it contains the category "flagged".
   scoresWellAnno = getArrayCorrectWellAnno(xsc)

    out <- data.frame(
#			firstCol = plate(xsc)
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
#		print(col)
#		print(ch)
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
				if (is.nan(r))
				{
					res["finalWellAnno"] = as.vector(scoresWellAnno[, 1, 1])   ## include also the final well annotation (after the screen log file)
					res
				}
				else
				{
					res[sprintf("finalWellAnno_r%d", r)] = as.vector(scoresWellAnno[, r, 1])   ## include also the final well annotation (after the screen log file)
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
#	selectSubList <- function(startName, endName)
#	{
#		startPos = idx[colOrder == startName]
##		browser()
#		ret = c()
##		mask = rep(FALSE, length(idx)) 
#		if (length(startPos) > 0)
#		{
#			endPos = idx[colOrder == endName]
#			for (i in 1:length(startPos))
#			{
#				interval = idx[startPos[i]:endPos[i]]
#				ret = c(ret, c(data.frame(start=startPos[i], end=endPos[i], interval=interval)))
##				mask=mask | (idx %in% interval)
#			}
#		}
##		return(data.frame(parts=ret, mask = mask))
#		return(ret)
#	}
#	chSubList = selectSubList(chGroupStart, chGroupEnd)
#	repSubList = selectSubList(repGroupStart, repGroupEnd)
#	simpleListsMask = rep(FALSE, colCount)
#	browser()
#	for (sub in c(chSubList, repSubList))
#	{#print(sub)
#		simpleListsMask = simpleListsMask | idx %in% sub
#	}
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


#	browser()
#	simpleListsMask = !simpleListsMask
#	print(simpleListsMask)
#	colOrder == "replicateGroupStart" | colOrder == "channelGroupStart"
#	subLists = (1:length(colOrder))[(1:length(colOrder))[colOrder == "replicateGroupStart"][1]:(1:length(colOrder))[colOrder == "replicateGroupEnd"][1]]
	
#	for (col in colOrder)
#	{
#		out <- addCol(col, 1, 1)
#	}

#	out["plate"]=plate(xsc)
#	out["position"]=position(xsc)
#	out["well"]=well(xsc)
#	out["score"]=as.vector(Data(xsc)[, 1, 1]) 
#	out["wellAnno"] = wAnno
#	out["finalWellAnno"] = as.vector(scoresWellAnno[, 1, 1])   ## include also the final well annotation (after the screen log file)
#	
#	
## add columns with 
#  if(!is.null(xraw)) {
#    ## Checks whether the number of channels has changed after normalization
#    originalNrCh <- dim(xraw)[3]
#
#    ## include also the raw values for each replicate and channel	 
#    out[sprintf("raw_r%d_%s", rep(1:nrReplicate, originalNrCh), rep(channels, each=nrReplicate))] <- sapply(1:originalNrCh, 
#           function(i) xraw[,,i])
#
#
#  for(ch in 1:originalNrCh) {
#    ## median between replicates (raw data) 
#    if(nrReplicate>1) {
#      out[sprintf("median_%s", channels[ch])] <- apply(out[sprintf("raw_r%d_%s", 1:nrReplicate, rep(channels[ch], nrReplicate))], 1, median, na.rm=TRUE)
#      if(nrReplicate==2) { 
#          ## Difference between replicates (raw data)
#          out[sprintf("diff_%s", channels[ch])] = apply(out[sprintf("raw_r%d_%s", 1:nrReplicate, rep(channels[ch], nrReplicate))], 1, diff)
#        } else {
#          ## average between replicates (raw data)
#          out[sprintf("average_%s", channels[ch])] = apply(out[sprintf("raw_r%d_%s", 1:nrReplicate, rep(channels[ch], nrReplicate))], 1, mean, na.rm=TRUE)
#        } 
#    }
#  }# for ch
#
#    ## raw/plateMedian
#    xrp <- array(as.numeric(NA), dim=dim(xraw))
#    isSample <- (as.character(wAnno) == "sample")
#    for(p in 1:nrPlate) {
#      indp <- (1:nrWell)+nrWell*(p-1)
#      samples <- isSample[indp]
#      xrp[indp,,] <- apply(xraw[indp,,,drop=FALSE], 2:3, function(j) j/median(j[samples], na.rm=TRUE))
#    }
#
#     out[sprintf("raw/PlateMedian_r%d_%s", rep(1:nrReplicate, originalNrCh), rep(channels, each=nrReplicate))] <- sapply(1:originalNrCh, 
#          function(i) {
#           signif(xrp[,,i], 3)  })
#
#  }
#
#  if(!is.null(xnorm)){
#    ## Include the normalized values
#    out[sprintf("normalized_r%d_%s", rep(1:nrReplicate, nrChannel), rep(channels, each=nrReplicate))] = sapply(1:nrChannel, 
#          function(i) round(xnorm[,,i], 3))
#  }
#  out["plates"]=plate(xsc)
#  out["positions"]=position(xsc)
#  out["wells"]=well(xsc)
  
#  paste(capture.output(print(dim(Data(xsc)))),collapse="\n")

#  for (ch in 1:nrChannel)#dim(Data(xsc))[3])
#  {
#	  out[sprintf("score_%s", channels[ch])] <- round(as.vector(Data(xsc)[, ch, ]), 3)
#  }
#
#  if(state(xsc)[["annotated"]]) {
#	  print(dim(fData(xsc)))
#	  print(names(fData(xsc)))
#	  n <- tolower(names(fData(xsc)))
#	  print(dim(out))
#	  print(names(out))
#	  out <- cbind(out, fData(xsc)[, !(n %in% tolower(c("controlStatus", "plate", "well", tolower(names(out)))))])
#  }
  
    ## Export everything to the file
#    out = out[order(out$score, decreasing=TRUE), ]
#    out$score = round(out$score, 2)
	firstCol = sprintf("score_%s", channels[1])
	if (firstCol %in% names(out))
	{
		out = out[order(out[[firstCol]], decreasing=TRUE), ]
#		out["score"] = round(out["score"], 2)
	}
	if (dim(out)[2] > 1)
	{
		out = out[, 2:(dim(out)[2])]
	}
	if(verbose) cat(sprintf("Saving 'topTable' list in file '%s'\n", file))
    write.tabdel(out, file=file)
    return(out)
}
