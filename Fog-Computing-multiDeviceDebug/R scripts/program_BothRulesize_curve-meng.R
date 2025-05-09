library(ggplot2)

working_dir <- "/Users/mengxu/Desktop/XUMENG/ZheJiangLab/ModifiedSimulation/submitToGrid/newModified20220222"
setwd(working_dir)

sprintf("------------------------Start------------------------------")
algos <- c("small", "middle","large")
devices <- c("1", "2", "3")
algo.names <- c("small", "middle","large")
#scenarios.name <- c("Nsmall1MTGP", "Nsmall2MTGP","Nsmall3MTGP","Nsmall4MTGP",
#                    "Nmiddle1MTGP", "Nmiddle2MTGP","Nmiddle3MTGP","Nmiddle4MTGP",
#                    "Nlarge1MTGP", "Nlarge2MTGP","Nlarge3MTGP","Nlarge4MTGP")

result.df <- data.frame(Scenario = character(),
                        Algo = character(),
                        Device = integer(),
                        Run = integer(),
                        Generation = integer(),
                        SeqRuleSize = integer(),
                        SeqRuleUniqueTerminals = integer(),
                        RoutRuleSize = integer(),
                        RoutRuleUniqueTerminals = integer(),
                        Obj = integer(),
                        TrainFitness = double(),
                        TestFitness = double(),
                        TrainTime = double()
)


for (a in 1:length(algos)) {
  algo <- algos[a]
  for (m in 1:length(devices)){
    scenario.name <- paste0("N",algo,m,"MTGP")
    #scenario.name <- scenarios.name[(a-1)*4+m]
    #scenario <- paste0(objectives[s], "-", utils[s], "-", ddfactors[s])
    testfile <- paste0("result.csv")
    df <- read.csv(paste0(algo, "/", scenario.name, "/results/test/", testfile), header = TRUE)
    result.df <- rbind(result.df,
                       cbind(Algo = rep(algo.names[a], nrow(df)),
                             Device = rep(devices[m], nrow(df)),
                             df))
  }
}
#}

runs <- unique(result.df$Run)
generations <- max(result.df$Generation)

testfit.df <- data.frame(Algo = character(),
                         Device = integer(),
                         Generation = integer(),
                         Mean = double(),
                         StdDev = double(),
                         StdError = double(),
                         ConfInterval = double())

for (a in 1:length(algos)) {
  algo <- algo.names[a]

  for (m in 1:length(devices)){
    device <- devices[m]
    for (g in 1:generations) {
      rows <- subset(result.df, Algo == algo & Device == device & Generation == g)

      if (nrow(rows) == 0)
        next

      rows.mean <- mean(rows$RoutRuleSize)
      rows.sd <- sd(rows$RoutRuleSize)
      rows.se <- rows.sd / sqrt(nrow(rows))
      rows.ci <- 1.96 * rows.sd

      testfit.df <- rbind(testfit.df, data.frame(Algo = algo,
                                                 Device = device,
                                                 Generation = g,
                                                 Mean = rows.mean,
                                                 StdDev = rows.sd,
                                                 StdError = rows.se,
                                                 ConfInterval = rows.ci))
    }
  }
}

testfitSeq.df <- data.frame(Algo = character(),
                         Device = integer(),
                         Generation = integer(),
                         Mean = double(),
                         StdDev = double(),
                         StdError = double(),
                         ConfInterval = double())

for (a in 1:length(algos)) {
  algo <- algo.names[a]

  for (m in 1:length(devices)){
    device <- devices[m]
    for (g in 1:generations) {
      rows <- subset(result.df, Algo == algo & Device == device & Generation == g)

      if (nrow(rows) == 0)
        next

      rows.mean <- mean(rows$SeqRuleSize)
      rows.sd <- sd(rows$SeqRuleSize)
      rows.se <- rows.sd / sqrt(nrow(rows))
      rows.ci <- 1.96 * rows.sd

      testfitSeq.df <- rbind(testfitSeq.df, data.frame(Algo = algo,
                                                 Device = device,
                                                 Generation = g,
                                                 Mean = rows.mean,
                                                 StdDev = rows.sd,
                                                 StdError = rows.se,
                                                 ConfInterval = rows.ci))
    }
  }
}
#}

testfit.df$Algo <- factor(testfit.df$Algo, levels = algos) #2020.10.20 order the appearrence of subplots
g <- ggplot() + geom_line(data = testfit.df, mapping = aes(Generation, Mean, colour = factor(Device))) + geom_point(data = testfit.df, mapping = aes(Generation, Mean, colour = factor(Device), shape = factor(Device))) +
  geom_line(data = testfitSeq.df, mapping = aes(Generation, Mean, colour = factor(Device), linetype = factor(Device))) + geom_point(data = testfitSeq.df, mapping = aes(Generation, Mean, colour = factor(Device), shape = factor(Device)))

#g <- g + facet_wrap(~ Scenario, nrow = 2, scales = "free")
#g <- g + facet_wrap(~ Scenario, ncol = 3, scales = "free")
g <- g + facet_wrap(~ Algo, ncol = 3, scales = "free")

g <- g + theme(legend.title = element_blank())
g <- g + theme(legend.position = "bottom")
g <- g + theme(legend.text = element_text(size = 19))

g <- g + labs(y = "routing rule size")

g <- g + theme(axis.title.x = element_text(size = 17, face = "bold"))
g <- g + theme(axis.title.y = element_text(size = 17, face = "bold"))
g <- g + theme(axis.text.x = element_text(size = 15))
g <- g + theme(axis.text.y = element_text(size = 15))
g <- g + theme(strip.text.x = element_text(size = 17))

#g <- g + theme(axis.title.x = element_text(size = 12, face = "bold"))
#g <- g + theme(axis.title.y = element_text(size = 12, face = "bold"))
#g <- g + theme(axis.text.x = element_text(size = 10))
#g <- g + theme(axis.text.y = element_text(size = 10))
#g <- g + theme(strip.text.x = element_text(size = 12))

ggsave("ruleSize-curve.pdf", width = 9, height = 3.5)
#ggsave("testfit-curve-noStd.pdf", width = 10, height = 5)

# table showing

finalTestFit.df <- data.frame(Algo = character(),
                              Device = integer(),
                              Run = integer(),
                              RoutRuleSize = double())

for (a in 1:length(algos)) {
  algo <- algo.names[a]
  for (m in 1:length(devices)){
    device <- devices[m]

    rows <- subset(result.df, Algo == algo & Device == device & Generation == generations)

    finalTestFit.df <- rbind(finalTestFit.df, data.frame(Algo = rep(algo, nrow(rows)),
                                                         Device = rep(device, nrow(rows)),
                                                         Run = rows$Run,
                                                         RoutRuleSize = rows$RoutRuleSize))
  }
}

for (s in 1:length(algos)) {
  rows1 <- subset(finalTestFit.df, Algo == algos[s] & Device == devices[1])
  rows2 <- subset(finalTestFit.df, Algo == algos[s] & Device == devices[2])
  rows3 <- subset(finalTestFit.df, Algo == algos[s] & Device == devices[3])
  rows4 <- subset(finalTestFit.df, Algo == algos[s] & Device == devices[4])

  cat(sprintf("%s
  & %.2f - %.2f(%.2f) & %.2f - %.2f(%.2f) & %.2f - %.2f(%.2f) & %.2f - %.2f(%.2f)\\\\\n",
              algos[s],
              min(rows1$RoutRuleSize), mean(rows1$RoutRuleSize), sd(rows1$RoutRuleSize),
              min(rows2$RoutRuleSize), mean(rows2$RoutRuleSize), sd(rows2$RoutRuleSize),
              min(rows3$RoutRuleSize), mean(rows3$RoutRuleSize), sd(rows3$RoutRuleSize),
              min(rows4$RoutRuleSize), mean(rows4$RoutRuleSize), sd(rows4$RoutRuleSize)))
}

