## ---------------------------------------------------------------------------
# LPB, August 2007
# Function to calculate per-plate dynamic range based on positive and negative controls.
# 	Arguments:
# 		x - cellHTS object
# 		definition - (optional) "difference" (to calculate the difference between the arithmetic average on positive and negative controls)
# 		                         "ratio" (to calculate the ratio between the geometric averages on positive and negative controls) - ATTENTION: only possible is data are in positive scale!
# 		verbose - TRUE or FALSE
#	        posControls  - optional (list or vector of regular expressions)
#		negControls - optional (vector of regular expressions)
#
## ---------------------------------------------------------------------------
getDynamicRange <- function(x, verbose=interactive(), definition, posControls, negControls) {

  ## consistency checks:
  if(!inherits(x, "cellHTS"))
    stop("'x' must be a 'cellHTS' object")

  ## Check the status of the 'cellHTS' object
  if(!state(x)["configured"])
    stop("Please configure 'x' (using the function 'configure') before calculating the dynamic range!")

  if (!is.logical(verbose))
    stop("'verbose' must be a logical value.")

  twoWay <- FALSE


  ## Get data and see how should the dynamic range be defined 
  y <- Data(x)
  allPositives <- ifelse(all(is.na(y)), TRUE, prod(range(y, na.rm=TRUE))>0)

  ##   -------  Definition for the dynamic range ---------------
  if(!missing(definition)) {
    if(!(definition %in% c("difference", "ratio"))) stop(sprintf("Undefined value %s for 'definition'! Please choose one of the following options: 'difference' or 'ratio'", definition))
    if(definition=="ratio" & !allPositives) stop("Please set 'definition' to 'difference', since data are not in positive scale!") 
  } else {
   ## define 'definition' based on the scale of the data: positive scale -> ratio of geometric average, otherwise, difference of arithmetic averages. 
   if (allPositives) definition="ratio" else definition="difference"
  }

  if(definition=="ratio") y <- log(y)

  ## dimensions
  d <- dim(y)
  nrWells    <- prod(pdim(x))
  nrPlates   <- max(plate(x))
  nrReplicates <- d[2]
  nrChannels <- d[3]

  wAnno <- as.character(wellAnno(x))

  ##   -------  Controls annotation ---------------
  if (!missing(posControls)) {

     ## checks, determine assay type and name of positive controls if assay is one-way
     namePos <- checkPosControls(posControls, nrChannels, wAnno, plateConf(x)$Content)
     twoWay <- namePos$twoWay
     namePos <- namePos$namePos 

  }else{## if !missing
    ## assumes the screen is a one-way assay
    posControls <- as.vector(rep("^pos$", nrChannels))
    namePos <- "pos"
  }

  if (!missing(negControls)) 
     checkControls(y=negControls, len=nrChannels, name="negControls") #check
  else  
     negControls <- as.vector(rep("^neg$", nrChannels))



  ##   -------  Get controls positions ---------------
  allControls <- getControlsPositions(posControls, negControls, twoWay, namePos, nrChannels, wAnno)

    actCtrls <- allControls$actCtrls
    inhCtrls <- allControls$inhCtrls
    posCtrls <- allControls$posCtrls
    negCtrls <- allControls$negCtrls


    nms <- if(twoWay) c("activators", "inhibitors") else namePos

## the line below is needed to have some feedback in 'writeReport'
    DR <- vector("list", length=max(1, length(nms)))
    names(DR) <- nms
    for(i in 1:length(DR)) { 
       DR[[i]] <- array(NA, dim=c(nrPlates, nrReplicates+1,nrChannels))
       colnames(DR[[i]]) <- c(paste("Replicate", 1:nrReplicates, sep=""), "Average")
    } 
  ##--------------------


  ##   -------  Calculate the per-plate dynamic ranges ---------------
  ## calculate quality metrics
  for (ch in 1:nrChannels) {

    # if there are no neg controls, dynamic range cannot be calculated!
    if(length(negCtrls[[ch]])) {

       yy <- matrix(y[,,ch, drop=FALSE], ncol=nrReplicates, nrow=d[1])
       neg <- ctrlsPerPlate(negCtrls[[ch]], nrWells)

      ##---------------------------------------
      ##  Two Way
      ##----------------------------------------
      if(twoWay){
        # get controls positions for current plate
        act <- ctrlsPerPlate(actCtrls[[ch]], nrWells)
        inh <- ctrlsPerPlate(inhCtrls[[ch]], nrWells)

        nms <- list()

        if(length(act)) nms$"activators" = c("act", "neg") else if(verbose) cat("\nNo 'activators' controls were found!\n\n")

        if(length(inh)) nms$"inhibitors" = c("neg", "inh") else if(verbose) cat("\nNo 'inhibitors' controls were found!\n\n")

        if(length(nms)) {

          for(i in 1:length(nms)) {
            ## 1.a) Dynamic range for activators and inhibitors (activators / neg or activators - neg) & Dynamic range (neg / inhibitors  or neg - inhibitors)
            platesWithData <- intersect(as.numeric(names(get(nms[[i]][1]))), as.numeric(names(get(nms[[i]][2]))))

            for(p in platesWithData) {
              # dyn range for each replicate
	      DR[[names(nms[i])]][p,1:nrReplicates, ch] <- apply(yy, 2, 
                  dynRange, get(nms[[i]][1])[[as.character(p)]], get(nms[[i]][2])[[as.character(p)]])
              # average dyn range for activators
              DR[[names(nms[i])]][p,nrReplicates+1,ch] <- dynRange(yy, get(nms[[i]][1])[[as.character(p)]], get(nms[[i]][2])[[as.character(p)]])
            } ##platesWithData
         } ## for nms
        } # length(nms)
     } else {
      ##---------------------------------------
      ##  One Way
      ##----------------------------------------
      notNull <- !sapply(posCtrls[[ch]], is.null)
      if(any(notNull)) {
        pp <- posCtrls[[ch]][notNull]
        ## for each different positive control:
        for (pname in names(pp)) {
          pos <- ctrlsPerPlate(pp[[pname]], nrWells)
          if(length(pos)) {
            platesWithData <- intersect(as.numeric(names(pos)), as.numeric(names(neg)))
 
          ## 1. Dynamic range (neg / pos controls)
          for (p in platesWithData) {
		# dyn range for each replicate
		DR[[pname]][p,1:nrReplicates,ch] <- apply(yy, 2, dynRange, pos[[as.character(p)]], neg[[as.character(p)]])
		# average dynamic range
		DR[[pname]][p,nrReplicates+1,ch] <- dynRange(yy, pos[[as.character(p)]], neg[[as.character(p)]])
	  } ## for platesWithData
      } # length(pos)
      }##  for names(posCtrls[[ch]])
    } else { ## any(notNull)
      if(verbose) cat("\nNo positive controls were found!\n\n")
    }
   } ## else twoWay
 } else { #length(neg)
    if(verbose) cat("\nNo negative controls were found!\n\n")
}
} ## for channel

  if(definition=="ratio") DR <- lapply(DR, exp)
  return(DR) 
}## function
## ---------------------------------------------------------------------------
