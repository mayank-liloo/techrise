const express = require('express');
const authRoutes = require('./routes/authRoutes');
const complaintRoutes = require('./routes/complaintRoutes');
const newsRoutes = require('./routes/newsRoutes');

const app = express();
app.use('/api/auth', authRoutes);
app.use('/api/complaints', complaintRoutes);
app.use('/api/news', newsRoutes);

console.log("Registered Routes:");
app._router.stack.forEach(function(r){
  if (r.route){ // route middleware
    console.log(r.route.path);
  } else if (r.name === 'router'){ // router middleware
    console.log('Router mounted at: ' + r.regexp);
    r.handle.stack.forEach(function(layer) {
      if (layer.route) {
        console.log('  ' + Object.keys(layer.route.methods).join(', ').toUpperCase() + ' ' + layer.route.path);
      }
    });
  }
});
