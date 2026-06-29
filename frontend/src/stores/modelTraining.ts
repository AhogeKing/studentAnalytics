import { defineStore } from "pinia";
import { trainDecisionTree } from "../api/model";
import type { ModelTrainMode, ModelTrainPayload, ModelTrainResult } from "../types";

let currentTrainingTask: Promise<ModelTrainResult> | null = null;

export const useModelTrainingStore = defineStore("modelTraining", {
  state: () => ({
    training: false,
    mode: null as ModelTrainMode | null,
    startedAt: null as string | null,
    lastTrain: null as ModelTrainResult | null,
    errorMessage: ""
  }),
  actions: {
    startTraining(payload: ModelTrainPayload) {
      if (currentTrainingTask) {
        return currentTrainingTask;
      }

      this.training = true;
      this.mode = payload.mode || "default";
      this.startedAt = new Date().toISOString();
      this.errorMessage = "";

      currentTrainingTask = trainDecisionTree(payload)
        .then((result) => {
          this.lastTrain = result;
          return result;
        })
        .catch((error: unknown) => {
          this.errorMessage = error instanceof Error ? error.message : "模型训练失败";
          throw error;
        })
        .finally(() => {
          this.training = false;
          this.mode = null;
          this.startedAt = null;
          currentTrainingTask = null;
        });

      return currentTrainingTask;
    }
  }
});
