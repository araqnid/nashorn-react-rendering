Server-side rendering using Nashorn and React
=============================================

This is a Java webapp, which aims to demonstrate "isomorphic" rendering- a one-page application where data is rendered server-side for the initial load, including consistent handling of URI paths for both server-side and client-side routing.

React.js is used to render based on some data model, both client and server side. In the server, the rendering is performed by running the components using the Nashorn JavaScript engine.

This is at a very early stage, and doesn't actually produce anything yet. In the test sources are demonstrations that Nashorn can in fact load React, transform JSX and render components. Some framework will be necessary to allow the routing outputs to be applied to the components both client- and server-side.

[![Build Status](https://travis-ci.org/araqnid/nashorn-react-rendering.svg?branch=master)](https://travis-ci.org/araqnid/nashorn-react-rendering)
