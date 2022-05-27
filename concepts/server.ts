import app from "./src/app";

const port = process.env.PORT ?? 3000;
app.listen(port, () => {
  console.log(`Concepts API listening on port ${port}`);
});
