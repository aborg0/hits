## ---------------------------------------------------------------------------
# LPB, August 2007
# Function to calculate per-experiment Z'-factor based on positive and negative controls (for each replicate and channel).
# 	Arguments:
# 		x - cellHTS object
#		robust - logical (TRUE or FALSE) - should the Z'-factor use robust measures of location and spread (median and MAD)?
# 		verbose - TRUE or FALSE
#	        posControls  - optional (list or vector of regular expressions)
#		negControls - optional (vector of regular expressions)
#
## ---------------------------------------------------------------------------
getZfactor <- function(x, robust=TRUE, verbose=interactive(), posControls, negControls) {

  ## consistency checks:
  if(!inherits(x, "cellHTS"))
    stop("'x' must be a 'cellHTS' object")

  ## Check the status of the 'cellHTS' object
  if(!state(x)["configured"])
    stop("Please configure 'x' (using the function 'configure') before calculating Z'-factor values!")

  if (!is.logical(verbose))
    stop("'verbose' must be a logical value.")

  if (!is.logical(robust))
    stop("'robust' must be a logical value.")

  twoWay <- FALSE

  ## Get data
  y <- Data(x)

  ## use mean and sd or median and mad?
  if (robust) {
    locationFun <- median 
    spreadFun <- mad
  } else {
    locationFun <- mean
    spreadFun <- sd
  } 


  ## dimensions
  d <- dim(y)
  nrReplicates <- d[2]
  nrChannels <- d[3]
  nrProbes <- d[1]
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
  #---------------------------------------------------------------------------------------------

  ##   -------  Get controls positions ---------------
    allControls <- getControlsPositions(posControls, negControls, twoWay, namePos, nrChannels, wAnno)

    actCtrls <- allControls$actCtrls
    inhCtrls <- allControls$inhCtrls
    posCtrls <- allControls$posCtrls
    negCtrls <- allControls$negCtrls

     nms <- if(twoWay) c("activators", "inhibitors") else namePos

# ## the line below is needed to have some feedback in 'writeReport'
     Zfac <- vector("list", length=max(1, length(nms)))
     names(Zfac) <- nms
     for(i in 1:length(Zfac)) { 
       Zfac[[i]] <- matrix(NA, nrow=nrReplicates, ncol=nrChannels)
       dimnames(Zfac[[i]]) <- list(paste("Replicate", 1:nrReplicates, sep=""), paste("Channel", 1:nrChannels, sep=""))
     } 
  ##--------------------

  nrPos = sapply(actCtrls, length) + sapply(inhCtrls, length) + sapply(posCtrls, function(w) length(unlist(w)))
  nrNeg = sapply(negCtrls, length) 

  ##   -------  Calculate the per-plate dynamic ranges ---------------
  ## calculate quality metrics
  for (ch in 1:nrChannels) {

    ## if there are no neg controls or no type of positive controls, Z'-factor cannot be calculated!
    if( nrPos[ch] & nrNeg[ch]) { 
       yy <- matrix(y[,,ch, drop=FALSE], ncol=nrReplicates, nrow=nrProbes)
       xact <- yy[actCtrls[[ch]],, drop=FALSE]
       xinh <- yy[inhCtrls[[ch]],, drop=FALSE]
       xpos = lapply(posCtrls[[ch]], function(d) yy[d, ,drop=FALSE])
       xneg = yy[negCtrls[[ch]], , drop=FALSE]
       nms <- xpos
       nms$activators<- xact
       nms$inhibitors <- xinh
       aux <- matrix(as.numeric(NA), ncol=length(nms), nrow=nrReplicates)
       colnames(aux) <- names(nms)
       for(dn in seq(along=nms))
         for (i in seq_len(ncol(nms[[dn]])))
           aux[i, dn] <- zfacFun(nms[[dn]][,i], xneg[,i], locationFun, spreadFun)

       inames <- which(colnames(aux) %in% names(Zfac) )
       for(i in inames) {
         nm <- colnames(aux)[i]
         Zfac[[nm]][,ch] <- aux[,i]
       }
 } else { #nrPos & nrNeg
    if(verbose) cat("\nNo positive and/or negative controls were found!\n\n")
}
} ## for channel

 return(Zfac) 
}## function
## ---------------------------------------------------------------------------

