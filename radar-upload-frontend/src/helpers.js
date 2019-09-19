export function downloadFile(filename, file, url) {
  const element = document.createElement('a');
  element.href = url || file;
  element.download = filename;
  element.target = '_blank';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}
export default {
  downloadFile,
};
