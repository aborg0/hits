## Functions for plate-by-plate normalization:
## perPlateScaling
## controlsBasedNormalization

## ===========================================================
## Auxiliary functions

funOperator=function(a,b, op) {
op = get(op, mode="function")
op(a,b)
}

POC = function(a, pos, plate, replicate, channel,...){

if(all(is.na(a[pos]))) stop(sprintf("No values for positive controls were found in plate %s, replicate %s, channel %d! Please use a different normalization function.", plate, replicate, channel))

100*a/mean(a[pos], na.rm=TRUE)
}

NPI = function(a, pos, neg, plate, replicate, channel){
if(all(is.na(a[pos])) | all(is.na(a[neg]))) stop(sprintf("No values for positive or/and negative controls were found in plate %s, replicate %s, channel %d! Please use a different normalization function.", plate, replicate, channel))

(mean(a[pos], na.rm=TRUE) - a)/(mean(a[pos], na.rm=TRUE) - mean(a[neg], na.rm=TRUE))
}


## ===========================================================

## 		----	perPlateScaling ------
##
## For each plate, calculates the ratio between each measurement and
## a certain plate intensity statistic (e.g. for scaleByPlateMean,
## the average intensity over the wells containing 'sample' or 'negative' controls).
##
## if data are in "additive" scale
## subtract by the per-plate correction factor
## instead of dividing by it.
##
## stats - character of length 1 giving the name of the plate intensity statistic to calculate. Options are: "mean", "median", "shorth" and "negatives"

perPlateScaling <- function(object, scale, stats="median", negControls){

  funArgs <- list(na.rm=TRUE)

  if(stats=="shorth") funArgs$tie.action="min"


  op <- ifelse(scale=="additive", "-", "/") 
  statFun <- if(stats=="negatives") median  else get(stats, mode="function")


  xnorm <- Data(object)

  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  wellAnnotation <- as.character(wellAnno(object))

  for(p in 1:nrPlates) {
    plateInds <- (1:nrWpP)+nrWpP*(p-1)
    wAnno = wellAnnotation[plateInds]
    inds <- (wAnno=="sample")
    for(ch in 1:nrChannels){

        if(stats=="negatives"){
          inds <- if(!(emptyOrNA(negControls[ch]))) findControls(negControls[ch], wAnno) else integer(0)

          if(!length(inds)) stop(sprintf("No negative controls were found in plate %s, channel %d! Please, use a different plate normalization method.", p, ch))
        }

        for(r in 1:nrSamples) {
         if(!all(is.na(xnorm[plateInds, r, ch]))) { 

          if(all(is.na(xnorm[plateInds, r, ch][inds])))
              stop(sprintf("No value for %s were found in plate %d, replicates %d, channel %d! Please %s", ifelse(stats=="negatives","negative controls", "samples"), p, r, ch, ifelse(stats=="negatives", "use a different plate normalization method.", "also flag the values for the controls in this plate!"))) 

          xnorm[plateInds,r,ch] <- funOperator(xnorm[plateInds, r, ch], do.call(statFun, c(list(x=xnorm[plateInds, r, ch][inds]), funArgs)), op)
      }#not all empty
  }# r
}# ch
       }# p
  Data(object) <- xnorm
  return(object)
}



## =========================================================================
##               ----- controlsBasedNormalization -----
## 
## General function that allows the following controls-based plate normalizations:
##       'POC' - Percent of control - determines the percentage of control, as the ratio between the 
## raw measurement and the mean of the measurements in the positive controls in an antagonist assay.
##
##       'NPI' - Normalized Percent Inhibition: for each plate, subtracts each measurement from the mean of the positive controls, and divides the result by the difference between the mean of positive and negative controls (plate dynamic range), in an antagonist assay.

controlsBasedNormalization <- function(object, method, posControls, negControls){

  xnorm <- Data(object)

  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  wellAnnotation <- as.character(wellAnno(object))
  fun <- get(method, mode="function")

  for(p in 1:nrPlates) {
    plateInds <- (1:nrWpP)+nrWpP*(p-1)
    wAnno <- wellAnnotation[plateInds]

      for(ch in 1:nrChannels) {
        if(!(emptyOrNA(posControls[ch]))) pos <- findControls(posControls[ch], wAnno) else pos <- integer(0)
        if(!(emptyOrNA(negControls[ch]))) neg <- findControls(negControls[ch], wAnno)  else neg <- integer(0)
        if(method == "POC") {
           if(!length(pos)) stop(sprintf("No positive controls were found in plate %s, channel %d! Please, use a different plate normalization method.", p, ch)) 
        } else {
	   if (!length(pos) | !length(neg)) stop(sprintf("No positive or/and negative controls were found in plate %s, channel %d! Please, use a different normalization function.", p, ch))
        }

	for(r in 1:nrSamples) 
         if(!all(is.na(xnorm[plateInds, r, ch])) )
            xnorm[plateInds, r, ch] = fun(xnorm[plateInds, r, ch], pos, neg, plate=p, replicate=r,channel=ch)

     } 
  }
  Data(object) <- xnorm
  return(object)
}


##-------- Bscore method (without plate variance adjustment) --------------------------
## B score: The residual (rijp) of the measurement for row i and column j on the p-th plate is obtained
## by fitting a two-way median polish:

## rijp = yijp - yijp_hat = yijp - (miu_hat + Rip_hat + Cjp_hat)

## For each plate p, the adjusted MADp is obtained from the rijp's.
## The variance adjustment step (Bscore = rijp/MADp) is omitted.


Bscore <- function(object, save.model=FALSE) {

  if(!inherits(object, "cellHTS")) stop("'object' should be of class 'cellHTS'.")
  
  if(!state(object)[["configured"]])
    stop("Please configure 'object' (using the function 'configure') before normalization.")


  ## data
  xdat <- Data(object)

  d <- dim(xdat)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  if(save.model) {
     rowcol.effects <- array(as.numeric(NA), dim=d)
     overall.effects <- array(as.numeric(NA), dim=c(nrPlates, d[2:3]))
  }

  isSample <- (wellAnno(object)=="sample")

  for(p in 1:nrPlates) {
    ## use only sample wells for the fit:
    plateInds <-(1:nrWpP)+nrWpP*(p-1)
    samples = isSample[plateInds]

    for(r in 1:nrSamples) {
      for(ch in 1:nrChannels) {
        ## y must be a numeric matrix with "plate rows" in rows and "plate columns" in columns:
        y <- ysamp <- xdat[plateInds, r, ch]
        if(!all(is.na(y))) {
          ysamp[!samples]=NA
          ysamp <- matrix(ysamp,
                          ncol=pdim(object)["ncol"],
                          nrow=pdim(object)["nrow"], byrow=TRUE)
          y = matrix(y,
            ncol=pdim(object)["ncol"], nrow=pdim(object)["nrow"], byrow=TRUE)
          m = medpolish(ysamp, eps = 1e-5, maxiter = 200, trace.iter=!TRUE, na.rm = TRUE)
          
          ## apply the model to all the plate wells and obtain the residuals rijp
          ## replace NA by zero:
          isNArow = is.na(m$row)
          isNAcol = is.na(m$col)
          isNA = outer(isNArow, isNAcol, "*")
          m$row[isNArow]=0
          m$col[isNAcol]=0
          rowcol = outer(m$row, m$col, "+")
          
          res = y - (m$overall + rowcol) 
          
          ## if the effect is NA in both column and row elements, restore the NA value:
          if (sum(isNA)) rowcol[as.logical(isNA)] = NA
          ## res is a matrix plate row * plate column
          xdat[plateInds, r, ch] <- as.vector(t(res))
          
          if (save.model) {
            rowcol.effects[plateInds,r,ch] <- as.vector(t(rowcol))
            overall.effects[p,r,ch]<-m$overall
          } # if
        } # if
      } # for ch
    }# for r
  } # for p

  if(save.model) {
    object@rowcol.effects <- rowcol.effects
    object@overall.effects <- overall.effects
  }

  Data(object) <- xdat
  object@state[["normalized"]] = TRUE
  validObject(object)
  return(object)
}


## ------- spatialNormalization ---------
spatialNormalization <- function(object, save.model=FALSE, ...){
   
  if(!inherits(object, "cellHTS"))
    stop("'object' should be of 'cellHTS' class.")
 
  if(!state(object)[["configured"]])
    stop("Please configure 'object' (using the function 'configure') before normalization.")

  xnorm <- Data(object)

  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  rowcol.effects <- array(as.numeric(NA), dim=d)
  posn <- 1:nrWpP

  ## Map the position in the plates into a (x,y) coordinates of a cartesian system
  ## having its origin at the centre of the plate
  row <- 1 +(posn-1) %/% pdim(object)[["ncol"]]
  col <- 1 + (posn-1) %% pdim(object)[["ncol"]]
  centre <- 0.5 + c(pdim(object)[["ncol"]]/2, pdim(object)[["nrow"]]/2) 
  xpos <- col - centre[1]
  ypos <- centre[2] - row

  wAnno <- wellAnno(object)

  for(p in seq_len(nrPlates)) {
    ## use only sample wells for the fit:
    plateInds = (1:nrWpP)+nrWpP*(p-1)
    isSample = (wAnno[plateInds]=="sample")

    for(r in 1:nrSamples)
      for(ch in 1:nrChannels){
        df = data.frame(y=xnorm[plateInds, r, ch], xpos=xpos, ypos=ypos)
        if (all(is.na(df$y)))
          next

        sdf = subset(df, isSample & (!is.na(df$y)))
        m = locfit(y ~ lp(xpos, ypos, ...), data = sdf, lfproc=locfit.robust)
        predx = predict(m, newdata=df)

        xnorm[plateInds, r, ch] = df$y - predx
        rowcol.effects[plateInds, r, ch] = predx
      }#for channel
  }#for replicate

  if (save.model) {
     object@rowcol.effects = rowcol.effects
     ## reset overall.effects to default of 'cellHTS' class to avoid problems in validity:
     object@overall.effects <-new("cellHTS")@overall.effects
   }

  object@state["normalized"] = TRUE
  Data(object) = xnorm
  validObject(object)
  return(object)
} 


