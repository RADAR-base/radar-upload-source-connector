#!/bin/sh

ROOT_DIR=/usr/share/nginx/html
TMP_DIR=/tmp/html

echo "Copying files to writable location..."
cp -r $ROOT_DIR $TMP_DIR

# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
for file in $TMP_DIR/js/app.*.js* $TMP_DIR/index.html;
do
  echo "Processing $file ...";

  sed -i 's|/\?VUE_APP_BASE_URL|'${VUE_APP_BASE_URL}'|g' $file
  sed -i 's|VUE_APP_API_BASE_URL|'${VUE_APP_API_BASE_URL}'|g' $file
  sed -i 's|VUE_APP_AUTH_API|'${VUE_APP_AUTH_API}'|g' $file
  sed -i 's|VUE_APP_AUTH_CALLBACK|'${VUE_APP_AUTH_CALLBACK}'|g' $file
  sed -i 's|VUE_APP_CLIENT_ID|'${VUE_APP_CLIENT_ID}'|g' $file
done

echo "Static files ready"
exec "$@"
