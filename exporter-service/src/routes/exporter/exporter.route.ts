import express from "express";
import { exportAsWord } from "./controller/exporterController.js";

const router = express.Router();

router.post("/word", exportAsWord);

export default router;
