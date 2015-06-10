## Store settings and state info in this internal environment
chtsSettings <- new.env(hash=FALSE)

## A list of default settings
chtsSettings[["report"]] <-
    list(controls=
         list(col=c(sample="black", neg="#377EB8", controls="#4DAF4A",
                                     other="#984EA3", empty="#FF7F00", flagged="#A65628",
                                     act="#E41A1C", inh="#FFFF33", pos="#E41A1C")),
         plateConfiguration=
         list(size=14, font="Helvetica", fontSize=12, thumbFactor=2,
              thumbFontSize=9, include=TRUE),
         plateSummaries=list(
              boxplot=list(
                  size=7.5, font="Helvetica", fontSize=12,
                  thumbFactor=1.5, thumbFontSize=11, col=c("pink", "lightblue")),
              controls=list(size=7.5, font="Helvetica", fontSize=12,
                  thumbFactor=1.5, thumbFontSize=11)),
         screenSummary=list(
              scores=list(size=7, font="Helvetica", fontSize=10,
                   thumbFactor=1, thumbFontSize=9,
                   col=list(posNeg=rev(brewer.pal(11, "RdBu"))[c(1:5, rep(6,3), 7:11)],
                            pos=brewer.pal(9, "Greys")),
                   aspect=1, annotation=NULL, map=FALSE, range=NULL),
              qqplot=list(size=7, font="Helvetica", fontSize=10,
                   thumbFactor=1, thumbFontSize=9),
              distribution=list(size=7, font="Helvetica", fontSize=10,
                   thumbFactor=1, thumbFontSize=9)),
         plateList=list(
              correlation=list(size=7.5, font="Helvetica", fontSize=14,
                   thumbFactor=1.5, thumbFontSize=12),
              maplot=list(size=7.5, font="Helvetica", fontSize=14,
                   thumbFactor=1.5, thumbFontSize=12),
              histograms=list(size=8, font="Helvetica", fontSize=14,
                   thumbFactor=1.8, thumbFontSize=10, type="histogram"),
              reproducibility=list(size=8, font="Helvetica", fontSize=12,
                   thumbFactor=1.3, thumbFontSize=10, col=brewer.pal(9, "YlOrRd"),
                   range=function(x) c(0, quantile(x, 0.95, na.rm=TRUE)),
                   include=FALSE, map=FALSE),
              average=list(size=8, font="Helvetica", fontSize=12,
                   thumbFactor=1.3, thumbFontSize=10, col=brewer.pal(9, "Greens"),
                   range=function(x) c(0, quantile(x, 0.95, na.rm=TRUE)),
                   include=FALSE, map=FALSE),       
              intensities=list(size=8, font="Helvetica", fontSize=12,
                   thumbFactor=1.6, thumbFontSize=10, col=rev(brewer.pal(9, "RdBu")),
                   #range=function(x) c(-1,1) * max(abs(x), na.rm=TRUE),
                   range=function(x) quantile(x, c(0.025, 0.975), na.rm = TRUE),
                   include=FALSE, map=FALSE)))
                                 

## Get the default session settings. Argument 'name' is suposed to be a character
## vector pointing in the default settings list, where nested list items can be
## addressed by vectors of length > 1. E.g., c("plateConfiguration", "size") would
## return the size setting for the plate configuration plot.
chtsGetSetting <- function(name=NULL) 
{
    lPars <- chtsSettings[["report"]]
    if (is.null(name))
    {
        return(lPars)
    }
    else
    {
        done <- FALSE
        for(n in name)
        {
            if(!done && n %in% names(lPars))
            {
                lPars <- lPars[[n]]
            }    
            else
            {
                lPars <- NULL
                done <- TRUE
            }
        }
        return(lPars)
    }
}

# Set the default session settings
chtsSetSetting <- function(value) 
{
    chtsSettings[["report"]] <- modifyList(chtsSettings[["report"]], value)
    invisible()
}


## These will get exposed through the API
setSettings <- function(x) chtsSetSetting(x)
getSettings <- function() chtsGetSetting()


