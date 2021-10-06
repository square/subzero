#!/usr/bin/env python3

"""Some simplistic assumptions to make life easy.

bits_per_pixel    memory layout
16                rgb565
24                rgb
32                argb
"""

import os
from PIL import Image
import argparse
import tempfile
import subprocess
import shlex

def _read_and_convert_to_ints(filename):
  with open(filename, "r") as fp:
    content = fp.read()
    tokens = content.strip().split(",")
    return [int(t) for t in tokens if t]


def _bw_to_argb(image: Image):
  return bytes([x for p in image.getdata()
                for x in (255, p, p, p)])


def _bw_to_rgb(image: Image):
  return bytes([x for p in image.getdata()
                for x in (p, p, p)])


def _bw_to_rgb565(image: Image):
  return bytes([(255 if x else 0) for p in image.getdata()
                for x in (p, p)])

_BW_CONVERTER = {
  16: _bw_to_rgb565,
  24: _bw_to_rgb,
  32: _bw_to_argb,
}


class Framebuf(object):
  """A framebuffer class.

  This helper class makes it slightly easy to deal with framebuffer
  """

  def __init__(self, device_node: str):
    """Initialize a framebuffer object from path to device node."""
    self.path = device_node
    fb_name = os.path.basename(device_node)
    config_dir = '/sys/class/graphics/%s/' % fb_name
    self.size = tuple(_read_and_convert_to_ints(
      config_dir + '/virtual_size'))
    self.stride = _read_and_convert_to_ints(config_dir + '/stride')[0]
    self.bits_per_pixel = _read_and_convert_to_ints(
      config_dir + '/bits_per_pixel')[0]
    assert self.stride == self.bits_per_pixel // 8 * self.size[0]

  def __str__(self):
    """Make printout prettier."""
    args = (self.path, self.size, self.stride, self.bits_per_pixel)
    return "%s  size:%s  stride:%s  bits_per_pixel:%s" % args

  def show(self, image: Image):
    """Draw image in framebuffer."""
    assert image.mode == '1', 'only mode 1 is supported'
    converter = _BW_CONVERTER[self.bits_per_pixel]
    assert image.size == self.size
    out = converter(image)
    with open(self.path, "wb") as fp:
      fp.write(out)


if __name__ == "__main__":
  parser = argparse.ArgumentParser("Create QR code from file, print to framebuffer.")
  parser.add_argument('device_node', metavar="device_node", type=str,
    help='path to framebuffer device node, e.g., /dev/fb0')
  parser.add_argument('infile', metavar="infile", type=str,
    help='path to input file to convert to QR code')

  args = parser.parse_args()
  dev_node = args.device_node
  infile = args.infile

  tmpfile = tempfile.NamedTemporaryFile(delete=False)

  cat = subprocess.Popen(('cat', infile), stdout=subprocess.PIPE)
  qrencode_cmd = 'qrencode -t PNG -8 -o ' + tmpfile.name
  cmd = shlex.split(qrencode_cmd)
  cmd_io = subprocess.check_output(cmd, stdin=cat.stdout)

  fb = Framebuf(dev_node)
  print (fb)
  image = Image.open(tmpfile.name).convert(mode="1")
  image.thumbnail(fb.size)
  print (image, image.mode)

  target = Image.new(mode="1", size=fb.size)
  assert image.size <= target.size
  # box is represented by upper left corner's coordinates
  box = ((target.size[0] - image.size[0]) // 2,
         (target.size[1] - image.size[1]) // 2)
  target.paste(image, box)
  fb.show(target)
