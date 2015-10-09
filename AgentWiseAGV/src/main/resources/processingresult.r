rm(list=ls())
library(Rmisc)
library(ggplot2)

pd <- position_dodge(1)

####################################

caBaselineMakeSpan = read.table("ResultsCABaseline_makespan.txt", header=TRUE)
caBaselineMakeSpan[,"approach"] = "ContextAwareBaseline"

caRepairMakeSpan = read.table("ResultsCARepair_makespan.txt", header=TRUE)
caRepairMakeSpan[,"approach"] = "ContextAwareRepair"

dMasMakeSpan30 = read.table("ResultsDMAS_makespan30paths.txt", header=TRUE)
dMasMakeSpan30[,"approach"] = "DelegateMAS30paths"

dMasMakeSpan10 = read.table("ResultsDMAS_makespan10paths.txt", header=TRUE)
dMasMakeSpan10[,"approach"] = "DelegateMAS10paths"

makespan_dat = rbind(caBaselineMakeSpan, caRepairMakeSpan, dMasMakeSpan30, dMasMakeSpan10)

makespan_datC = summarySE(makespan_dat, measurevar="makespan", groupvars=c("numAGVs", "approach"))

ggplot(makespan_datC, aes(x=numAGVs, y=makespan, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=makespan-sd, ymax=makespan+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=makespan_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmakespan.pdf', width = 8, height = 4)

####################################

caBaselinePlanCost = read.table("ResultsCABaseline_plancost.txt", header=TRUE)
caBaselinePlanCost[,"approach"] = "ContextAwareBaseline"

caRepairPlanCost = read.table("ResultsCARepair_plancost.txt", header=TRUE)
caRepairPlanCost[,"approach"] = "ContextAwareRepair"

dMasPlanCost30 = read.table("ResultsDMAS_plancost30paths.txt", header=TRUE)
dMasPlanCost30[,"approach"] = "DelegateMAS30paths"

dMasPlanCost10 = read.table("ResultsDMAS_plancost10paths.txt", header=TRUE)
dMasPlanCost10[,"approach"] = "DelegateMAS10paths"

planCost_dat = rbind(caBaselinePlanCost, caRepairPlanCost, dMasPlanCost30, dMasPlanCost10)

planCost_datC <- summarySE(planCost_dat, measurevar="PlanCost", groupvars=c("numAGVs", "approach"))

ggplot(planCost_datC, aes(x=numAGVs, y=PlanCost, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=PlanCost-sd, ymax=PlanCost+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=planCost_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicplancost.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfMakeSpanCABaseline = c()
percentageOfMakeSpanCARepair = c()
percentageOfMakeSpanDMAS30 = c()
percentageOfMakeSpanDMAS10 = c()

for (i in 1:100) {
  numAGVs[i] = caBaselineMakeSpan[i,1]
  percentageOfMakeSpanCABaseline[i] = 100
  percentageOfMakeSpanCARepair[i] = (caRepairMakeSpan[i,2] / caBaselineMakeSpan[i,2])*100
  percentageOfMakeSpanDMAS30[i] = (dMasMakeSpan30[i,2] / caBaselineMakeSpan[i,2])*100
  percentageOfMakeSpanDMAS10[i] = (dMasMakeSpan10[i,2] / caBaselineMakeSpan[i,2])*100
}

percentageMSCABaseline = data.frame(numAGVs, percentageOfMakeSpanCABaseline)
colnames(percentageMSCABaseline)[2] = "percentage"
percentageMSCARepair = data.frame(numAGVs, percentageOfMakeSpanCARepair)
colnames(percentageMSCARepair)[2] = "percentage"
percentageMSDMAS30 = data.frame(numAGVs, percentageOfMakeSpanDMAS30)
colnames(percentageMSDMAS30)[2] = "percentage"
percentageMSDMAS10 = data.frame(numAGVs, percentageOfMakeSpanDMAS10)
colnames(percentageMSDMAS10)[2] = "percentage"

percentageMSCABaseline[,"approach"] = "ContextAwareBaseline"
percentageMSCARepair[,"approach"] = "ContextAwareRepair"
percentageMSDMAS30[,"approach"] = "DMAS30paths"
percentageMSDMAS10[,"approach"] = "DMAS10paths"

percentageMS = rbind(percentageMSCABaseline, percentageMSCARepair, percentageMSDMAS30, percentageMSDMAS10)

percentageMSC <- summarySE(percentageMS, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentageMSC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentageMSC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan DMAS / Makespan CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmakespanpercentage.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfPlanCostCABaseline = c()
percentageOfPlanCostCARepair = c()
percentageOfPlanCostDMAS30 = c()
percentageOfPlanCostDMAS10 = c()

for (i in 1:100) {
  numAGVs[i] = caBaselinePlanCost[i,1]
  percentageOfPlanCostCABaseline[i] = 100
  percentageOfPlanCostCARepair[i] = (caRepairPlanCost[i,2] / caBaselinePlanCost[i,2])*100
  percentageOfPlanCostDMAS30[i] = (dMasPlanCost30[i,2] / caBaselinePlanCost[i,2])*100
  percentageOfPlanCostDMAS10[i] = (dMasPlanCost10[i,2] / caBaselinePlanCost[i,2])*100
}

percentagePCCABaseline = data.frame(numAGVs, percentageOfPlanCostCABaseline)
colnames(percentagePCCABaseline)[2] = "percentage"
percentagePCCARepair = data.frame(numAGVs, percentageOfPlanCostCARepair)
colnames(percentagePCCARepair)[2] = "percentage"
percentagePCDMAS30 = data.frame(numAGVs, percentageOfPlanCostDMAS30)
colnames(percentagePCDMAS30)[2] = "percentage"
percentagePCDMAS10 = data.frame(numAGVs, percentageOfPlanCostDMAS10)
colnames(percentagePCDMAS10)[2] = "percentage"

percentagePCCABaseline[,"approach"] = "ContextAwareBaseline"
percentagePCCARepair[,"approach"] = "ContextAwareRepair"
percentagePCDMAS30[,"approach"] = "DMAS30paths"
percentagePCDMAS10[,"approach"] = "DMAS10paths"

percentagePC = rbind(percentagePCCABaseline, percentagePCCARepair, percentagePCDMAS30, percentagePCDMAS10)

percentagePCC <- summarySE(percentagePC, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentagePCC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentagePCC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost DMAS / Plancost CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicplancostpercentage.pdf', width = 8, height = 4)

####################################

caBaselineMakeSpan = read.table("ResultsMultiCA_makespan.txt", header=TRUE)
caBaselineMakeSpan[,"approach"] = "ContextAwareBaseLine"

caRepairMakeSpan = read.table("ResultsMultiCArepair_makespan.txt", header=TRUE)
caRepairMakeSpan[,"approach"] = "ContextAwareRepair"

dMasMakeSpan = read.table("ResultsMultiDMAS_makespan.txt", header=TRUE)
dMasMakeSpan[,"approach"] = "DelegateMAS"

makespan_dat = rbind(caBaselineMakeSpan, dMasMakeSpan, caRepairMakeSpan)

makespan_datC = summarySE(makespan_dat, measurevar="makespan", groupvars=c("numAGVs", "approach"))

ggplot(makespan_datC, aes(x=numAGVs, y=makespan, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=makespan-sd, ymax=makespan+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=makespan_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultimakespan.pdf', width = 8, height = 4)

####################################

caBaselinePlanCost = read.table("ResultsMultiCA_plancost.txt", header=TRUE)
caBaselinePlanCost[,"approach"] = "ContextAwareBaseLine"

caRepairPlanCost = read.table("ResultsMultiCArepair_plancost.txt", header=TRUE)
caRepairPlanCost[,"approach"] = "ContextAwareRepair"

dMasPlanCost = read.table("ResultsMultiDMAS_plancost.txt", header=TRUE)
dMasPlanCost[,"approach"] = "DelegateMAS"

planCost_dat = rbind(caBaselinePlanCost, dMasPlanCost, caRepairPlanCost)

planCost_datC <- summarySE(planCost_dat, measurevar="PlanCost", groupvars=c("numAGVs", "approach"))

ggplot(planCost_datC, aes(x=numAGVs, y=PlanCost, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=PlanCost-sd, ymax=PlanCost+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=planCost_datC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultiplancost.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfMakeSpanCABaseline = c()
percentageOfMakeSpanCARepair = c()
percentageOfMakeSpanDMAS = c()

for (i in 1:100) {
  numAGVs[i] = caBaselineMakeSpan[i,1]
  percentageOfMakeSpanCABaseline[i] = 100
  percentageOfMakeSpanCARepair[i] = (caRepairMakeSpan[i,2] / caBaselineMakeSpan[i,2])*100
  percentageOfMakeSpanDMAS[i] = (dMasMakeSpan[i,2] / caBaselineMakeSpan[i,2])*100
}

percentageMSCABaseline = data.frame(numAGVs, percentageOfMakeSpanCABaseline)
colnames(percentageMSCABaseline)[2] = "percentage"
percentageMSCARepair = data.frame(numAGVs, percentageOfMakeSpanCARepair)
colnames(percentageMSCARepair)[2] = "percentage"
percentageMSDMAS = data.frame(numAGVs, percentageOfMakeSpanDMAS)
colnames(percentageMSDMAS)[2] = "percentage"

percentageMSCABaseline[,"approach"] = "ContextAwareBaseline"
percentageMSCARepair[,"approach"] = "ContextAwareRepair"
percentageMSDMAS[,"approach"] = "DMAS"

percentageMS = rbind(percentageMSCABaseline, percentageMSCARepair, percentageMSDMAS)

percentageMSC <- summarySE(percentageMS, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentageMSC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentageMSC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Makespan DMAS / Makespan CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultimakespanpercentage.pdf', width = 8, height = 4)

####################################

numAGVs = c()
percentageOfPlanCostCABaseline = c()
percentageOfPlanCostCARepair = c()
percentageOfPlanCostDMAS = c()

for (i in 1:100) {
  numAGVs[i] = caBaselinePlanCost[i,1]
  percentageOfPlanCostCABaseline[i] = 100
  percentageOfPlanCostCARepair[i] = (caRepairPlanCost[i,2] / caBaselinePlanCost[i,2])*100
  percentageOfPlanCostDMAS[i] = (dMasPlanCost[i,2] / caBaselinePlanCost[i,2])*100
}

percentagePCCABaseline = data.frame(numAGVs, percentageOfPlanCostCABaseline)
colnames(percentagePCCABaseline)[2] = "percentage"
percentagePCCARepair = data.frame(numAGVs, percentageOfPlanCostCARepair)
colnames(percentagePCCARepair)[2] = "percentage"
percentagePCDMAS = data.frame(numAGVs, percentageOfPlanCostDMAS)
colnames(percentagePCDMAS)[2] = "percentage"

percentagePCCABaseline[,"approach"] = "ContextAwareBaseline"
percentagePCCARepair[,"approach"] = "ContextAwareRepair"
percentagePCDMAS[,"approach"] = "DMAS"

percentagePC = rbind(percentagePCCABaseline, percentagePCCARepair, percentagePCDMAS)

percentagePCC <- summarySE(percentagePC, measurevar="percentage", groupvars=c("numAGVs", "approach"))

ggplot(percentagePCC, aes(x=numAGVs, y=percentage, color=approach, group=approach)) + 
  geom_errorbar(aes(ymin=percentage-sd, ymax=percentage+sd), width=3, position=pd) +
  geom_line(position=pd) +
  geom_point(size=3, position=pd) +
  theme_classic() + 
  scale_x_discrete(limits=percentagePCC[,1]) +
  theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
  coord_cartesian(xlim = c(5, 105)) +
  xlab('Number of AGVs') +
  ylab('Plancost DMAS / Plancost CA') + 
  theme(legend.title=element_blank()) +
  theme(legend.justification=c(0,1), legend.position=c(0, 1))

ggsave('dynamicmultiplancostpercentage.pdf', width = 8, height = 4)

# #######################################
# caMultiStage = read.table("ResultsMultiCA.txt", header=TRUE)
# caMultiStage[,"approach"] = "ContextAware"
# 
# dMasMultiStage = read.table("ResultsMultiDMAS.txt", header=TRUE)
# dMasMultiStage[,"approach"] = "DelegateMAS"
# 
# throughput_dat = rbind(caMultiStage, dMasMultiStage)
# 
# throughput_datC = summarySE(throughput_dat, measurevar="FinishedTask", groupvars=c("numAGVs", "approach"))
# 
# ggplot(throughput_datC, aes(x=numAGVs, y=FinishedTask, color=approach, group=approach)) + 
#   geom_errorbar(aes(ymin=FinishedTask-sd, ymax=FinishedTask+sd), width=3, position=pd) +
#   geom_line(position=pd) +
#   geom_point(size=3, position=pd) +
#   theme_classic() + 
#   scale_x_discrete(limits=throughput_datC[,1]) +
#   theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
#   coord_cartesian(xlim = c(5, 105)) +
#   xlab('Number of AGVs') +
#   ylab('Throughput') + 
#   theme(legend.title=element_blank()) +
#   theme(legend.justification=c(0,1), legend.position=c(0, 1))
# 
# ggsave('dynamicthrougput.pdf', width = 8, height = 4)
# 
# ################################################
# 
# numAGVs = c()
# percentageOfThroughPut = c()
# 
# for (i in 1:nrow(caMultiStage)) {
#   numAGVs[i] = caMultiStage[i,1]
#   percentageOfThroughPut[i] = (dMasMultiStage[i,2] / caMultiStage[i,2])*100
# }
# 
# percentageThroughPut = data.frame(numAGVs, percentageOfThroughPut)
# 
# percentageThroughPutC <- summarySE(percentageThroughPut, measurevar="percentageOfThroughPut", groupvars=c("numAGVs"))
# 
# ggplot(percentageThroughPutC, aes(x=numAGVs, y=percentageOfThroughPut)) + 
#   geom_errorbar(aes(ymin=percentageOfThroughPut-sd, ymax=percentageOfThroughPut+sd), width=3, position=pd) +
#   geom_line(position=pd) +
#   geom_point(size=3, position=pd) +
#   theme_classic() + 
#   scale_x_discrete(limits=percentageMSC[,1]) +
#   theme(panel.border = element_rect(color = "black", fill = NA, size = 1)) + 
#   coord_cartesian(xlim = c(5, 105)) +
#   xlab('Number of AGVs') +
#   ylab('Throughput DMAS / Throughput CA') + 
#   theme(legend.title=element_blank()) +
#   theme(legend.justification=c(0,1), legend.position=c(0, 1))
# 
# ggsave('dynamicthrougputpercentage.pdf', width = 8, height = 4)