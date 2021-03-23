process.on('unhandledRejection', (err) => {
  console.log(err);
  // eslint-disable-next-line no-undef
  fail(err);
});
