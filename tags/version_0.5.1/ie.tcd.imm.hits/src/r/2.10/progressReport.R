## This functions updates the progress report on the command line. Arguments are
##   si: integer indicating the current step
##   smax: integer indicating the total number of steps
##   timeCounter: the total elapsed time
##   additionalTime: the amount of time that is to be added to the total elapsed time
##   tmax: the total time needed to finish the report
## Note that this feature will not work on Windows since the hacked event loop prevents the
## console from updating.
myUpdateProgress <- function(time, currentStep, additionalTime=time$timePerStep[currentStep])
{
    if(!is.na(additionalTime))
        time$timeCounter <- time$timeCounter + additionalTime
    time$currentStep <- match(currentStep, time$steps2Do)
    cat(sprintf("\r%d%% done (step %s of %d)", min(floor(time$timeCounter/time$totalTime*100), 100),
                as.character(min(time$currentStep, time$nsteps)), time$nsteps))
    return(invisible(time))
}



## A list containing the rough estimation of the total computation time for the writeReport function
## and the current ellapsed time, 1 = one time unit	
createProgressList <- function(nrReplicate, nrChannel, nrPlate, plotPlateArgs, xr, overallState){
    progress <- list()
    progress$timeCounter <- 0
    progress$timePerStep <- c(
                              step0=0,
                              step1=15,
                              step2=nrPlate*nrReplicate*nrChannel*(1 + if(is.list(plotPlateArgs)) 3 +
                              plotPlateArgs$map else 0),
                              step3=0.1*sum(plateList(xr)$Status=="OK") + 2*nrChannel*nrReplicate,
                              step4=8*nrChannel*nrReplicate, 
                              step5=20*nrChannel*nrReplicate,
                              step6=nrPlate*(0.5) * nrChannel,
                              step7=5
                              )				
    progress$steps2Do <- names(progress$timePerStep)[c(TRUE, rep(overallState[["configured"]],2), TRUE, TRUE,
                                                       rep(overallState[["scored"]],2))]
    progress$totalTime <- sum(progress$timePerStep[progress$steps2Do])
    progress$nsteps <- length(progress$steps2Do)
    progress$currentStep <- 1
    return(progress)
}


