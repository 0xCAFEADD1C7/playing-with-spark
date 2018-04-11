

const fs = require('fs');
const csv = require('csv-js');

function haversineDistance([lon1, lat1], [lon2, lat2]) {
  function toRad(x) {
    return x * Math.PI / 180;
  }

  var R = 6371; // km

  var x1 = lat2 - lat1;
  var dLat = toRad(x1);
  var x2 = lon2 - lon1;
  var dLon = toRad(x2)
  var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  var d = R * c;

  return d;
}

// 31 eme

const fcontent = fs.readFileSync("data/Hospitals.csv", "utf8");
const rowsHosp = csv.parse(fcontent);
const hospPos = rowsHosp.map(row => [row[19], row[18]]).slice(1)

function nearestHosp (acc) {
  // console.log("ACC",acc);
  const minDst = hospPos.reduce((best, cur) => {
    const lonlat = [acc[26], acc[25]]
    const dst = haversineDistance(lonlat, cur)
    // console.log("DST", dst)
    return Math.min(dst, best);
  }, Infinity);
  // console.log("MDS", minDst)
  return minDst
}

const accs = csv.parse(fs.readFileSync("data/accident.csv", "utf8"))

const headers = accs.slice(0,1);
const rows = accs.slice(1);

headers.push("NRST_HOSP");
const accsWithHosp = rows.map(acc => acc.concat(nearestHosp(acc)));

console.log(headers.join(","))
accsWithHosp.forEach(acc=> {
  console.log(acc.join(","));
})


