writehref = function(x, url, con)
  cat(sprintf("<A HREF=\"%s\">%s</A>", url, x), file=con)

writeheader = function(x, level, con)
    cat(sprintf("<HTML><HEAD><TITLE>%s</TITLE></HEAD>\n<BODY><CENTER><H%d>%s</H%d></CENTER>\n\n",
                as.character(x), as.integer(level), as.character(x), as.integer(level)), file=con)

writeExperimentHeader = function(xy, x, y, url, level, con)
    cat(sprintf("<HTML><HEAD><TITLE>%s</TITLE></HEAD>\n<BODY><CENTER><H%d>%s<A HREF=\"%s\">%s</A></H%d></CENTER>\n\n",
                as.character(xy), as.integer(level), as.character(x), url,  as.character(y), as.integer(level)), file=con)

writetail = function(con)
    cat(sprintf("<BR><HR>%s</HTML></HEAD>\n", date()), file=con)


# ------------------------------------------------------------
writeHTMLtable = function(x, url, con,
  colors = c("#e0e0ff", "#d0d0f0", "#f0f0ff", "#e0e0f0"), center=FALSE, extra=NULL) {

  if(!is.data.frame(x))
    stop("'x' must be a data.frame")
  nr = nrow(x)
  nc = ncol(x)
  if(!missing(url)) {
    if(! (is.matrix(url) && is.character(url) && nrow(url)==nr && ncol(url)==nc))
      stop("'url' must be a character matrix of the same size as 'x'")
    for(j in 1:nc)
      x[, j] = ifelse(is.na(url[, j]), x[, j], sprintf("<A HREF=\"%s\">%s</A>", url[, j], x[, j]))
  }


  if(center) cat("<CENTER>\n", file=con)
    if (!is.null(extra)){
       nn = (nc-1)/length(extra)
       cat("<TABLE border=0><TR>", sprintf("<TH BGCOLOR=\"%s\"> </TH>", colors[1]), paste(sprintf("<TH colspan=%d align=center BGCOLOR=\"%s\">%s</TH>", nn, rep(colors[1], length(extra)), extra), collapse=""), "</TR>\n", sep="", file=con)

       cat("<TR>", paste(sprintf("<TH BGCOLOR=\"%s\">%s</TH>", colors[(1:nc)%%2+1], colnames(x)), collapse=""),"</TR>\n", sep="", file=con)
    } else {
      cat("<TABLE border=0><TR>",
      paste(sprintf("<TH BGCOLOR=\"%s\">%s</TH>", colors[(1:nc)%%2+1], colnames(x)), collapse=""),
      "</TR>\n", sep="", file=con) 
    }

  for(i in 1:nr)
#     cat("<TR>", paste(sprintf("<TD BGCOLOR=\"%s\" align=center>%s</TD>", colors[2*(i%%2)+(1:nc)%%2+1], x[i,]), collapse=""),
#         "</TR>\n", sep="", file=con)
	if (is.null(extra))
	{
		cat("<TR>",
			paste(sprintf("<TD BGCOLOR=\"%s\">%s</TD>", colors[2*(i%%2)+(1)%%2+1], x[i,1]), collapse=""),
			paste(sprintf("<TD BGCOLOR=\"%s\" align=center>%s</TD>", colors[2*(i%%2)+(2:nc)%%2+1], x[i,-1]), collapse=""),
        "</TR>\n", sep="", file=con)
	} else
	{
		cat("<TR>", paste(sprintf("<TD BGCOLOR=\"%s\">%s</TD>", colors[2*(i%%2)+(1)%%2+1], x[i,1]), collapse=""), sep="", file=con)
		for (j in 1:(nc-1))
		{
			cat(sprintf("<TD BGCOLOR=\"%s\" align=center>%s</TD>", colors[2*(i%%2)+(j)%%2+1], x[i,-1][[j]]), file=con)
		}
		cat("<TR>\n", file=con)
	}

  cat("</TABLE>\n", file=con)
  if(center) cat("</CENTER>\n", file=con)
}
#-----------------------------------------------------------------



writeHTMLtable4plots = function(x, con,
  colors = c("#e0e0ff", "#d0d0f0", "#f0f0ff", "#e0e0f0")) {

  nr = nrow(x)
  nc = ncol(x)

  cat("<CENTER><TABLE border=0><TR>",
      paste(sprintf("<TH BGCOLOR=\"%s\">%s</TH>", colors[(1:nc)%%2+1], names(x)), collapse=""),
      "</TR>\n", sep="", file=con)

  for(i in 1:nr) {
    cat("<TR>", paste(paste("<TD BGCOLOR=\"", colors[2*(i%%2)+(1:nc)%%2+1],
                            "\">", x[i,], "</TD>", sep=""), collapse=""),
        "</TR>\n", sep="", file=con)
         }
  cat("</TABLE><CENTER>\n", file=con)
}

##
 myUpdateProgress <- function(ti, tmax, si, smax){
       updateProgress(ti/tmax*100, sub=sprintf("step %s of %s", si, smax), autoKill=!TRUE)
 }



##----------------------------------------------------------------------------
writeReport = function(cellHTSlist,
  outdir,
  #outdir=file.path(getwd(), name(x)),
  force=FALSE, map=FALSE, 
  plotPlateArgs=FALSE,
  imageScreenArgs=NULL, 
  progressReport=interactive(),
  posControls,
  negControls,
  channels=paste("Channel", 1:dim(cellHTSlist[["raw"]])[2]),
  colOrder=defaultColOrder()
  ) {

##############################
## NOTE: 'writeReport' can be called on different cellHTS objects at different preprocessing stages
## Arguments:
## 'cellHTSlist'  should be a list of cellHTS object(s) obtained for the same experimental data. Allowed components are:
	## 'raw' - (mandatory) cellHTS object containing raw experiment data.
        ## 'normalized' (mandatory only if component 'scored' is given)- cellHTS object containing normalized data.
        ## 'scored' - cellHTS object comprising scored data.
## e.g. cellHTSlist = list("raw" = xr, "normalized"=xn, "scored"=xsc)

allowedListNames <- c("raw", "normalized", "scored")

if(!is.list(cellHTSlist)) {
   stop("Argument 'cellHTSlist' should be a list containing one or a maximum of 3 'cellHTS' objects.") 
} else {


 if(!all(sapply(cellHTSlist, class)=="cellHTS")) stop("Argument 'cellHTSlist' should be a list of cellHTS objects!")

 nm <- names(cellHTSlist)
 if(!("raw" %in% nm)) stop("Argument 'cellHTSlist' should be a list containing at least one component named 'raw' that corresponds to a 'cellHTS' object containing unnormalized data.")

 if(length(cellHTSlist)>3 | any(duplicated(nm))) stop("Argument 'cellHTSlist' can only have a maximum of 3 components named 'raw', 'normalized' and 'scored'!")

 if(!all(nm %in% allowedListNames)) 
     stop(sprintf("Invalid named component%s in argument 'cellHTSlist': %s", 
         ifelse(sum(!(nm %in% allowedListNames))>1, "s", ""), 
                       nm[!(nm %in% allowedListNames)]))
}

xr <- cellHTSlist[["raw"]]
xn <- cellHTSlist[["normalized"]]
xsc <- cellHTSlist[["scored"]]


# now check whether the given components of 'cellHTSlist' are valid cellHTS objects:
  if(any(state(xr)[c("scored", "normalized")])) stop(sprintf("The component 'raw' of argument 'cellHTSlist' should be a 'cellHTS' object containing unnormalized data!\nPlease check its preprocessing state: %s", paste(names(state(xr)), "=", state(xr), collapse=", ")))

   if(!is.null(xn)) {
     if(!(state(xn)[["normalized"]] & !state(xn)[["scored"]])) stop(sprintf("The component 'normalized' of 'cellHTSlist' should be a 'cellHTS' object containing normalized data!\nPlease check its preprocessing state: %s", paste(names(state(xn)), "=", state(xn), collapse=", ")))

   if(!compare2cellHTS(xr, xn)) stop("'cellHTS' objects contained in dat[['raw']] and dat[['normalized']] are not from the same experiment!")
  }

  if(!is.null(xsc)) {
   if(!state(xsc)["scored"]) stop(sprintf("The component 'scored' of argument 'cellHTSlist' should be a 'cellHTS' object containing scored data!\nPlease check its preprocessing state: %s", paste(names(state(xsc)), "=", state(xsc), collapse=", ")))

   if(!compare2cellHTS(xr, xsc)) stop("Difference across 'cellHTS' objects! The scored 'cellHTS' object given in dat[['scored']] was not calculated from the data stored in 'cellHTS' object indicated in 'dat[['raw']]'!")

  # If 'scored' component was given, than 'normalized' component should also be available!
  if(is.null(xn)) stop("Please add to 'cellHTSlist' list a component named 'normalized' corresponding to a cellHTS object containing the normalized data!") 
  }

## --------------------------------------


  ## consistency checks:
  if (!is.logical(progressReport))
    stop("'progressReport' must be a logical value.")

  if (!is.logical(map))
    stop("'map' must be a logical value.")

  if(is.logical(plotPlateArgs)) {
    if(plotPlateArgs)
      plotPlateArgs <- list(map=map)
  } else {
    if(!is.list(plotPlateArgs)) {
      stop("'plotPlateArgs' must either be logical or a list.") 
     } else {
      if(!all(names(plotPlateArgs) %in% c("sdcol", "sdrange", "xcol", "xrange", "map")))
      stop("Only elements 'sdcol', 'sdrange', 'xcolx' and 'xrange' are allowed for 'plotPlateArgs' list!")
      plotPlateArgs$map = map
     }
  }

  if(is.list(imageScreenArgs)) {
    if(!("map" %in% names(imageScreenArgs)))
      imageScreenArgs$map = map
    if(!all(names(imageScreenArgs) %in% c("ar", "zrange", "map","anno")))
      stop("Only elements 'ar', 'zrange', 'map' and 'anno'are allowed for 'imageScreenArgs' list!")

  } else {
    if(!is.null(imageScreenArgs)) 
      stop("'imageScreenArgs' must either be a list or NULL.") else imageScreenArgs=list(map=map)
  }

    # available data
    xraw <- Data(xr)  ## xraw should always be given!
    xnorm <- if(is.null(xn)) xn else Data(xn)
#    scores <- if(is.null(xsc)) xsc else Data(xsc)


  # dimensions 
  d <- as.integer(dim(xraw))
  nrWell    <- prod(pdim(xr))
  nrPlate   <- max(plate(xr))
  nrReplicate <- as.numeric(d[2])
  nrChannel <- if(!is.null(xnorm)) as.integer(dim(xnorm)[3]) else d[3]  ## will be defined based on xnorm, if it exists

  objState <- sapply(cellHTSlist, function(i) {
                    if(!is.null(i)) state(i) }
                    )

overallState <- apply(objState, 1, any)
#whConfigured <- colnames(objState)[objState["configured",]]
whAnnotated <- colnames(objState)[objState["annotated",]]


  ## Progress bar 
  ## Rough estimation of the total computation time that the function will take
  ## 1 = one time unit
  ## Steps inside writeReport:
  	# Step 1 - creating the output directory
  	# Step 2 - Controls annotation (only if overallState["configured"]=TRUE)
  	# Step 3 - QC per plate & channel (only if overallState(x)["configured"]=TRUE)
  	# Step 4 - Add plate result files and write the overall QC results
  	# Step 5 - Per experiment QC
  	# Step 6 - topTable  (only if scored data are available)
  	# Step 7 -  Screen-wide image plot (only if scored data are available)

  if (progressReport){
    timeCounter=0
    timePerStep <- c(
      step1 = 5,
      step2 = 5,
      step3 = nrPlate*nrReplicate*nrChannel*(1 + if(is.list(plotPlateArgs)) 3 + plotPlateArgs$map else 0),
      step4 = 0.2*sum(plateList(xr)$status=="OK") + 4*nrChannel*nrReplicate,
      step5 = 10*nrChannel*nrReplicate, 
      step6 = 20*nrChannel*nrReplicate,
      step7 = nrPlate*(0.5 + imageScreenArgs$map)*nrChannel
      )

    steps2Do <- names(timePerStep)[c(TRUE, rep(overallState[["configured"]],2), TRUE, TRUE, rep(overallState[["scored"]],2))]
    totalTime <- sum(timePerStep[steps2Do])
    nsteps <- length(steps2Do)

    require("prada")
#     progress(title="cellHTS is busy", message = sprintf("\nCreating HTML pages for '%s'. \nState: \n%s \n%s", name(x), 
#              paste(paste(names(state(x))[1:2], state(x)[1:2], sep="="), collapse=", "), 
#              paste(paste(names(state(x))[3:4], state(x)[3:4], sep="="), collapse=", ")), sub=sprintf("step %s of %s", 1, nsteps))

     progress(title="cellHTS2 is busy", message = sprintf("\nCreating HTML pages for '%s'. \nFound %s data.\nState:\n%s",       		name(xr), 
                if(length(cellHTSlist)>1)  paste(paste(nm[-length(cellHTSlist)], collapse=", "), "and",  nm[length(cellHTSlist)],  collapse=" ") else nm ,
                paste(paste("configured", overallState[["configured"]], sep=" = "), paste("annotated", overallState[["annotated"]], sep=" = "), sep=", ")),
     sub=sprintf("step %s of %s", 1, nsteps))

    on.exit(killProgress(), add=TRUE)
  }


  ## Create the output directory
  ## See if output directory exists. If not, create. If yes, check if it is empty,
  ## and if not, depending on parameter 'force', throw an error or clean it up.
  if(missing(outdir))
    outdir = file.path(getwd(), name(xr))

  if(file.exists(outdir)){
    if(!file.info(outdir)$isdir)
      stop(sprintf("'%s' must be a directory.", outdir))
    outdirContents = dir(outdir, all.files = TRUE)
    outdirContents = setdiff(outdirContents, c(".", ".."))
    if(  (length(outdirContents)>0L) && !force )
      stop(paste(sprintf("The directory '%s' is not empty.", outdir),
                 "Please empty the directory manually, or use the argument 'force=TRUE' to overwrite.\n", sep="\n"))
  } else {
    dir.create(outdir, recursive=TRUE)
  }

  indexFile = file.path(outdir, "index.html") 
  con = file(indexFile, "w")
  on.exit(close(con), add=TRUE)

  dir.create(file.path(outdir, "in"))

  ## Create header for the HTML report & add description file if 'x' is configured
  if(overallState["configured"]) {
     nm = file.path("in", "Description.txt")

     writeLines(screenDesc(xr), file.path(outdir, nm))
     writeExperimentHeader(paste("Experiment report for ", name(xr)), "Experiment report for ", name(xr), nm, 1, con)
  } else { 
     writeheader(paste("Experiment report for", name(xr)), 1, con)
  }


  if(progressReport){
   #stepNr = 1
   timeCounter <- timeCounter + timePerStep["step1"]
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step1", steps2Do)+1, nsteps)
  }


 ## initializations
 twoWay <- FALSE
 wAnno = as.character(wellAnno(xr))


  ## the overview table of the plate result files in the experiment,
  ##   plus the (possible) urls for each table cell
  exptab = plateList(xr)
  url = matrix(as.character(NA), nrow=nrow(exptab), ncol=ncol(exptab))
  colnames(url) = colnames(exptab)
  qmHaveBeenAdded = FALSE


## -------------------------
 if(overallState["configured"]) {

  if(progressReport) {
    timeCounter=timeCounter+2
    updateProgress(100*timeCounter/totalTime, autoKill = !TRUE)
  }

  ##   -------  Step 2) Controls annotation ---------------
  if (!missing(posControls)) {
     ## checks, determine assay type and name of positive controls if assay is one-way
     namePos <- checkPosControls(posControls, nrChannel, wAnno, plateConf(xr)$Content)
     twoWay <- namePos$twoWay
     namePos <- namePos$namePos 

  }else{## if !missing
    ## assumes the screen is a one-way assay
    posControls <- as.vector(rep("^pos$", nrChannel))
    namePos <- "pos"
  }

  if (!missing(negControls)) 
     checkControls(y=negControls, len=nrChannel, name="negControls") #check
  else  
     negControls <- as.vector(rep("^neg$", nrChannel))
  #---------------------------------------------------------------------------------------------



  if(progressReport){
   #stepNr = 2
   timeCounter <- timeCounter + timePerStep["step2"]
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step2", steps2Do)+1, nsteps)
  }



    ## Define the bins for the histograms (channel-dependent)
    brks = apply(if(overallState["normalized"]) { xnorm } else { xraw },
      3, range, na.rm=TRUE)
    brks = apply(brks, 2, function(s) pretty(s, n=ceiling(nrWell/10))) 
    if(!is(brks, "list")) brks=split(brks, col(brks))
    ## put as list also for the case ch=1 or for the case when brks have equal length for each channel 


   ## Correct wellAnno information:
     ## ... by taking into account the wells that were flagged in the screen log file, 
     ## or even by the user manually in xraw. Besides the categories in wellAnno(x), it contains the category "flagged".
   xrawWellAnno = getArrayCorrectWellAnno(xr)
   # put as array with dimensions nrWells x nrPlates x nrReplicates x nrChannels
   xrawWellAnno = array(xrawWellAnno, dim=c("Wells"=nrWell, "Plates"=nrPlate, nrReplicate, dim(xrawWellAnno)[3])) # don't use variable 'nrChannel' because it may be different when defined based on xnorm data!

  ## Create geneAnnotation info for the image maps:
  if(overallState["annotated"]){
       # follow the order 'scored' - 'normalized' - 'raw'
       for(i in c("scored", "normalized", "raw")) {
         if(i %in% whAnnotated) {
           screenAnno <- fData(cellHTSlist[[i]]) 
           break
         }
       }

    geneAnnotation <- if ("GeneSymbol" %in% names(screenAnno))  screenAnno$GeneSymbol else screenAnno$GeneID

  }else{##else if annotated
    geneAnnotation <- well(xr)
  }##else annotated


       ## data
       if(overallState["normalized"]) {
          dat <- xnorm
          whatDat = "normalized"
        } else {
          dat <- xraw
          whatDat <- "unnormalized"
        }

  ## which of the replicate plates has not just NA values
  datPerPlate <- array(dat, dim=c("Wells"=nrWell, "Plates"=nrPlate, nrReplicate, nrChannel)) 
  hasData <- apply(datPerPlate, 2:4, function(z) !all(is.na(z))) # nrPlates x nrReplicates x nrChannels

  ##   -------  Get controls positions (for all plates) ---------------
    allControls <- getControlsPositions(posControls, negControls, twoWay, namePos, nrChannel, wAnno)
    actCtrls <- allControls$actCtrls
    inhCtrls <- allControls$inhCtrls
    posCtrls <- allControls$posCtrls
    negCtrls <- allControls$negCtrls

  ## get controls positions for each plate
     act <- lapply(actCtrls, function(i) if(is.null(i)) NULL else ctrlsPerPlate(i, nrWell))
     inh <- lapply(inhCtrls, function(i) if(is.null(i)) NULL else ctrlsPerPlate(i, nrWell))
     neg <- lapply(negCtrls, function(i) if(is.null(i)) NULL else ctrlsPerPlate(i, nrWell))
     pos <- vector("list", length=nrChannel)
      for (ch in 1:nrChannel) {
        notNull <- !sapply(posCtrls[[ch]], is.null)
        if(any(notNull)) {
          pp <- posCtrls[[ch]][notNull]
          pos[[ch]] <- lapply(pp, ctrlsPerPlate, nrWell)
          } 
         }



  ## -----------  Get per-plate dynamic range ----------
  ## -----------  Get per-plate repeatability standard deviation (plate replicates) ----------
  ## -----------  Get Z'-factor for each replicate and channel ---------- (needed as input for QMexperiment later on)
  if(whatDat=="normalized") {
     dr <- getDynamicRange(xn, verbose=FALSE, posControls=posControls, negControls=negControls)
     repMeasures <- getMeasureRepAgreement(xn, corr.method="spearman")
     allZfac <- getZfactor(xn, verbose=FALSE, posControls=posControls, negControls=negControls) 

     #dr <- getDynamicRange(xn, verbose=FALSE, allControls=allControls, twoWay=twoWay, namePos=namePos) 
     #allZfac <- getZfactor(xn, verbose=FALSE, allControls=allControls, twoWay=twoWay, namePos=namePos) 

  } else { #use cellHTS object containing raw data
     dr <- getDynamicRange(xr, verbose=FALSE, posControls=posControls, negControls=negControls)
     repMeasures <- getMeasureRepAgreement(xr, corr.method="spearman")
     allZfac <- getZfactor(xr, verbose=FALSE, posControls=posControls, negControls=negControls) 
#      dr <- getDynamicRange(xr, verbose=FALSE, allControls=allControls, twoWay=twoWay, namePos=namePos)
#      repMeasures <- getMeasureRepAgreement(xr, corr.method="spearman")
#      allZfac <- getZfactor(xr, verbose=FALSE, allControls=allControls, twoWay=twoWay, namePos=namePos) 
 }

if(all(is.null(names(dr)))) names(dr) <- namePos

  ## Define well colors and comment on them.
  ## (to avoid having the legend for 'pos' when we have 'inhibitors' and 'activators' or vice-versa)
  wellTypeColor <- if(twoWay) c(neg="#2040FF", act="#E41A1C", inh="#FFFF00", sample="#000000", controls="#43FF00",
                   other="#999999", empty="#FA00FF", flagged="#000000") else c(pos="#E41A1C", neg="#2040FF", sample="#000000", controls="#43FF00", other="#999999", empty="#FA00FF", flagged="#000000")
 
  ## assign common arguments for the plate plots
  if(is.list(plotPlateArgs)) {
    ## Currently, it does not allows to use different colors for different channels
    if(is.null(plotPlateArgs$sdcol)) 
      plotPlateArgs$sdcol = brewer.pal(9, "YlOrRd")
    if(is.null(plotPlateArgs$xcol))
      plotPlateArgs$xcol = rev(brewer.pal(9, "RdBu"))

    ## set this argument as a list with the same length as the number of channels
    if(is.null(plotPlateArgs$xrange)) { 
      plotPlateArgs$xrange <- vector("list", length=dim(dat)[3])
    } else {
        if (!is.list(plotPlateArgs$xrange)) {
          plotPlateArgs$xrange <- list(plotPlateArgs$xrange)
	  length(plotPlateArgs$xrange)=dim(dat)[3]} 
    }

    ## set this argument as a list with the same length as the number of channels
    if(is.null(plotPlateArgs$sdrange)) {
      plotPlateArgs$sdrange <- vector("list", length=dim(dat)[3])
    } else {
        if (!is.list(plotPlateArgs$sdrange)) {
          plotPlateArgs$sdrange <- list(plotPlateArgs$sdrange)
          length(plotPlateArgs$sdrange) = dim(dat)[3]
        } 
    }

  } #is.list(plotPlateArgs)


  ##   -------  Step 3)  QC per plate & channel ---------------
  allmt <- match(wAnno, names(wellTypeColor))

   for(p in 1:nrPlate){
      wh = with(plateList(xr), which(Plate==p & status=="OK"))
      if(length(wh)>0) {
        dir.create(file.path(outdir, p))

        res <- QMbyPlate(platedat=datPerPlate[, p,,, drop=FALSE], 
          pdim=pdim(xr), 
          name=sprintf("Plate %d (%s)", p, whatDat),
          basePath=outdir, 
          subPath=p, 
          genAnno=geneAnnotation[nrWell*(p-1)+(1:nrWell)], 
          mt=allmt[nrWell*(p-1)+(1:nrWell)],
          plotPlateArgs=plotPlateArgs, 
          brks = brks,
          finalWellAnno = xrawWellAnno[,p,,, drop=FALSE], 
          activators=act, inhibitors=inh, positives=pos, negatives=neg, 
          isTwoWay=twoWay, namePos=namePos, wellTypeColor=wellTypeColor,
          plateDynRange=lapply(dr, function(i) i[p,,,drop=FALSE]), 
          plateWithData=hasData[p,,, drop=FALSE], repMeasures=repMeasures, channels=channels)

        url[wh, "status"] = res$url

        if(!qmHaveBeenAdded) {
          if(twoWay){
            TableNames = c(paste("Replicate dynamic range", c("(Activators)", "(Inhibitors)"), sep=" "), paste("Average dynamic range", c("(Activators)", "(Inhibitors)"), sep=" "), "Spearman rank correlation")
          }else{## if twoWay

            if(length(namePos)==1 && namePos=="pos") 
              TableNames = c("Replicate dynamic range", "Average dynamic range", "Repeatability standard deviation", sprintf("Spearman rank correlation %s", ifelse(nrReplicate==2, "", "(min - max)")))
            else
              TableNames = c(sprintf("Replicate dynamic range (%s)", namePos), 
              sprintf("Average dynamic range (%s)", namePos), "Repeatability standard deviation", 
              sprintf("Spearman rank correlation %s", ifelse(nrReplicate==2, "", "(min - max)")))
          }## else twoWay
          url = cbind(url,  matrix(as.character(NA), nrow=nrow(url), ncol=length(TableNames)))

          for (j in TableNames) exptab[, j] = rep("", nrow(exptab))
          qmHaveBeenAdded = TRUE
        }## if !qmHaveBeenAdded
        whh = split(wh, exptab$Channel[wh])
       
        for(ch in 1:length(res$qmsummary)) { # Channels
          resCh = res$qmsummary[[ch]]
          whCh = whh[[ch]]
          selrep= exptab$Replicate[whCh]
          if(twoWay){
            for (jj in 1:length(TableNames))
               exptab[whCh, TableNames[jj]] = resCh[unique((jj<3)*(selrep+nrReplicate*(jj-1))) + (jj>2)*(nrReplicate*2 + jj-2)] 
                #"Replicate dynamic range (Activators)"
                #"Replicate dynamic range (Inhibitors)"
                #TableNames[3] "Average dynamic range (Activators)"
                #TableNames[4] "Average dynamic range (Inhibitors)"
                #TableNames[5] "Repeatability standard deviation"
                #TableNames[6] "Spearman rank correlation"

          }else{ #oneway

            for (jj in 1:(length(TableNames)-2)) #exclude "Repeatability standard deviation" and "Spearman rank correlation"
              exptab[whCh, TableNames[jj]] = resCh[unique((jj<(length(namePos)+1))*(selrep + (nrReplicate+1)*(jj-1))) + (jj>length(namePos))*(nrReplicate + 1)*(jj-length(namePos))]

            exptab[whCh, TableNames[length(TableNames)-1]] = resCh[length(resCh)-1]
            exptab[whCh, TableNames[length(TableNames)]] = resCh[length(resCh)]
          }## else twoWay
        }## for channel
      }## if length w

    # update the progress bar each time a plate is completed:
    if(progressReport){
      #stepNr = 3
      timeCounter <- timeCounter + timePerStep["step3"]/nrPlate
      myUpdateProgress(timeCounter, totalTime, match("step3", steps2Do), nsteps)
    }

   }## for p plates


 # after completing all plates:
 if(progressReport){
   #stepNr = 3
   #timeCounter <- timeCounter + timePerStep[paste("step",stepNr,sep="")]/nrPlate
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step3", steps2Do)+1, nsteps)
  }
}## if configured


  ##   -------  Step 4) Add plate result files and write with overall QC results -------------
  ##  Report pages per plate result file 
  wh = which(plateList(xr)$status=="OK")
  nm = file.path("in", names(intensityFiles(xr)))
  for(w in wh) {
    txt = intensityFiles(xr)[[w]]
    if(is.null(txt))
      stop(sprintf("CellHTS object is internally inconsistent, plate %d (%s) is supposedly OK but has no raw data file.",
                   as.integer(w), nm[w]))
    writeLines(txt, file.path(outdir, nm[w]))
    url[w, "Filename"] = nm[w]

   ### time for step4 : 0.2*sum(x@plateList$status=="OK") + 4*nrChannel*nrReplicate

   # update progress bar each time w is updated:
   if(progressReport){
     #stepNr = 4
     timeCounter <- timeCounter + 0.2
     myUpdateProgress(timeCounter, totalTime, match("step4", steps2Do), nsteps)
   }
 
  } # for w


  # write table with overall CQ results
  cat("<CENTER>", file=con)
  exptab[5] = rep(channels, nrPlate * nrReplicate)
  writeHTMLtable(exptab, url=url, con=con)
  cat("</CENTER><BR><BR>", file=con)

# End of step 4 - update progress bar
 if(progressReport){
   #stepNr = 4
   timeCounter <- timeCounter + 4*nrChannel*nrReplicate
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step4", steps2Do)+1, nsteps)
  }


##   ----------------------
## Add the screen plot with plate configuration 
## (only based on the content of plate configuration file! No updates based on screen log file.)
  if(state(xr)[["configured"]]) {
    ## Create a data.frame for the screen plot with plate configuration
    res <- makePlot(outdir, con=con, name="configurationAsScreenPlot",
                    w=7, h=7*pdim(xr)["nrow"]/pdim(xr)["ncol"]*ceiling(nrPlate/6)/6+0.5, psz=8,
                    fun = function() {
                     do.call("configurationAsScreenPlot", 
                         args=list(x=xr, verbose=FALSE, posControls=unlist(posControls), negControls=negControls))
                     },
                    print=FALSE, isImageScreen=FALSE)
# do plot with the legend
makePlot(outdir, con=con, name="colLeg", w=5, h=2, psz=8, fun=function(){ image(matrix(1:length(res)), axes=FALSE, col=res, add = !TRUE, ylab="", xlab="Color scale"); axis(1, at = seq(0,1,length=length(res)), tick = !TRUE, labels=names(res)) }, print=FALSE, isImageScreen=FALSE)

   confTable = data.frame(matrix(data = NA, nrow = 2, ncol = 1))
   names(confTable) = "Plate configuration"

#   wellLeg <- paste(sprintf("<FONT COLOR=\"%s\">%s</FONT>", res, names(res)), collapse=", ")
#   wellLeg <- sprintf("<CENTER><em>Color legend: </em><br> %s</CENTER><BR>\n", wellLeg)
#   confTable[1, 1] = sprintf("%s<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n",
#                 wellLeg, "configurationAsScreenPlot.pdf", "configurationAsScreenPlot.png")
  confTable[1,1] <- sprintf("<CENTER><IMG SRC=\"%s\"/></CENTER>\n", "colLeg.png")

  confTable[2,1] <- sprintf("<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n", "configurationAsScreenPlot.pdf", "configurationAsScreenPlot.png")

   cat("<CENTER>", file=con)
   cat("</CENTER><BR><BR>", file=con)
   writeHTMLtable4plots(confTable, con=con)
   cat("<BR><BR>", file=con)
  }


  ##   -------  Step 5)  Per experiment QC ---------------
  plotTable = QMexperiment(xr, xn, outdir, con, allControls, allZfac, channels=channels)

 
 if(progressReport){
   #stepNr = 5
   timeCounter <- timeCounter + timePerStep["step5"]
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step5", steps2Do)+1, nsteps)
  }




  if(overallState[["scored"]]) {
    nrChannels <- dim(Data(xsc))[2]
    if(overallState["annotated"]) ttInfo = "Table of scored <BR> and annotated probes" else ttInfo = "Table of scored probes"

    ##   -------  Step 6)  topTable ---------------
    out <- getTopTable(cellHTSlist, file=file.path(outdir, "topTable.txt"), verbose=FALSE, channels=channels, colOrder=colOrder)


    if(progressReport){
       #stepNr = 6
       timeCounter <- timeCounter + timePerStep["step6"]
       # print cumulative time for last step and print number of next step:
       myUpdateProgress(timeCounter, totalTime, match("step6", steps2Do)+1, nsteps)
    }

  ##   -------  Step 7)  Screen-wide image plot ---------------
    count = nrow(plotTable)
  for (ch in 1:nrChannel)
  {
    res <- makePlot(outdir, con=con, name=sprintf("imageScreen_ch%d", ch), w=7, h=7, psz=8,
                    fun = function(map=imageScreenArgs$map)
                      do.call("imageScreen", args=append(list(object=xsc, map=map, channel=ch), imageScreenArgs[!names(imageScreenArgs) %in% "map"])),
                    print=FALSE, isImageScreen=TRUE)

    plotTable = rbind(plotTable, rep("", length=prod(ncol(plotTable)* 2))) 
    plotTable[count + 1, 1+ch] = paste("<H3 align=center>Screen-wide image plot of the scored values (", channels[ch], ")</H3>")
    plotTable[count + 2, 1] = sprintf("<CENTER><A HREF=\"topTable.txt\"><em>%s</em></A></CENTER><BR>\n", ttInfo)

    if (is.null(res)) {

      plotTable[count + 2, ch - 1 + 2] = sprintf("<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER><BR>\n", sprintf("imageScreen_ch%d.pdf", ch), sprintf("imageScreen_ch%d.png", ch))
    }else{

      # Update with first part of step 7 - update progress bar
      if(progressReport){
        #stepNr = 7
        timeCounter <- timeCounter + nrPlate*0.5
        # print cumulative time for last step and print number of next step:
        myUpdateProgress(timeCounter, totalTime, match("step7", steps2Do), nsteps)
      }

      res <- myImageMap(object=res$obj, tags=res$tag, sprintf("imageScreen_ch%d.png", ch))
      plotTable[count + 2, ch - 1 + 2] = paste("<BR><CENTER>", res, "</CENTER><BR><CENTER>",
              sprintf("<A HREF=\"imageScreen_ch%d.pdf\">enlarged version</A></CENTER>\n", ch), sep="")
    }
  }
  } ## if scored

  writeHTMLtable4plots(plotTable, con=con)


 if(progressReport){
   #stepNr = 7
   timeCounter <- totalTime #timeCounter + timePerStep["step7"]
   # print cumulative time for last step and print number of next step:
   myUpdateProgress(timeCounter, totalTime, match("step7", steps2Do), nsteps)
  }

  writetail(con)
  return(indexFile)
}
