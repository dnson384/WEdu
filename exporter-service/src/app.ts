import express from "express";
import exporterRouter from "./routes/exporter/exporter.route.js";

const app = express();

app.use(express.json({ limit: "50mb" }));

app.use("/exporter", exporterRouter);

app.listen(8000, () => {
  console.log("Server started on port 8000");
});
