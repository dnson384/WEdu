import express from "express";
import { exportAsWord } from "./controller/exporterController.js";

const router = express.Router();

router.post("/exam", exportAsWord);

export default router;
