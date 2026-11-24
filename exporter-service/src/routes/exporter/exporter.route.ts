import express from "express";
import { exportAsWord, exportMatrixAsWord } from "./controller/exporterController.js";

const router = express.Router();

router.post("/exam", exportAsWord);
router.post("/matrix", exportMatrixAsWord);

export default router;
