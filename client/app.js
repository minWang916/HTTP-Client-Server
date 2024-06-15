const express = require('express');

var app = express()
app.use(express.urlencoded());
app.use(express.json());
app.set('view engine', 'ejs');

async function readAllChunks(readableStream) {
  const reader = readableStream.getReader();
  const chunks = [];
  
  let done, value;
  while (!done) {
    ({ value, done } = await reader.read());
    if (done) {
      return chunks;
    }
    chunks.push(value);
  }
}

app.listen(5000, () => {
  console.log("Listening on port 5000");
});

app.get("/", async function(req, res) {
  var response = await fetch('http://localhost:8080/demo/multithread/hi.txt');
  console.log(response.headers);
  var data = await readAllChunks(response.body);
  var string = new TextDecoder().decode(data[0]);
  res.send(string);
});

app.get("/img", async function(req, res) {
  var response = await fetch('http://localhost:8080/demo/multithread/dog.png');
  var data = await readAllChunks(response.body);
  let length = 0;
  data.forEach(item => {
    length += item.length;
  });
  let mergedArray = new Uint8Array(length);
  let offset = 0;
  data.forEach(item => {
    mergedArray.set(item, offset);
    offset += item.length;
  });
  console.log(data);
  res.render('index', {data: mergedArray});
});


