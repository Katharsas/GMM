import sys

# consoles suck dick and python throws up if you print unicode without this (WHY?)
def uprint(*objects, sep=' ', end='\n', file=sys.stdout):
	"""
	From stackoverflow: http://stackoverflow.com/a/29988426
	Print to console in unicode wether the console likes it or not.
	Worst case: Console will print the code point numbers...
	
	Windows: use command "chcp 65001" to enable unicode (and use proper font)
	"""
	enc = file.encoding
	if enc == 'UTF-8':
		print(*objects, sep=sep, end=end, file=file)
	else:
		f = lambda obj: str(obj).encode(enc, errors='backslashreplace').decode(enc)
		print(*map(f, objects), sep=sep, end=end, file=file)

# start with
# blender --background --python <script.py> -- <script args>
# e.g.:
# blender --background --python ../../src/main/python/test.py

import bpy, os, tempfile

# check blender out

try:
	bpy.ops.wm.addon_enable(module="io_three")
except ImportError:
	print("Threejs export addon not found")
	
if hasattr(bpy.types, "EXPORT_OT_three"):
	print("Three.js Export Plugin is available.")
else:
	sys.exit(0)
	
# create server

import socketserver

class MyRequestHandler(socketserver.BaseRequestHandler):
	
	def myInit(self):
		self.init = None
		self.endSignal = "\n"
		self.encoding = "utf-8"
		self.byteBuffer = b""
		self.unicodeBuffer = ""
	
	def next(self):
		"""
		Returns the String or None if a complete String is not avaliable yet.
		"""
		if len(self.byteBuffer) > 0:
			try:
				newUnicode = self.byteBuffer.decode(self.encoding)
				self.byteBuffer = b""
				self.unicodeBuffer += newUnicode
			except UnicodeError:
				pass
		splitted = self.unicodeBuffer.split(self.endSignal, 1)
		if len(splitted) <= 1:
			return None
		else:
			self.unicodeBuffer = splitted[1]
			return splitted[0]
	
	def getStringFromClient(self):
		"""
		Blocks until client sends String followed by endSignal.
		Returns the String without endSignal.
		"""
		try:
			self.init = self.init
		except AttributeError:
			self.myInit()
		result = None
		while result is None:
			self.request.settimeout(10.0); # make sure recv doesnt get stuck
			self.byteBuffer += self.request.recv(1024)
			result = self.next()
		return result
	
	def handle(self):
		"""
		Handles a complete client connection.
		After this method returns, client cannot send without new connection.
		"""
		result = self.getStringFromClient()
		uprint(result)
		self.request.sendall(result.encode(self.encoding))
		result = self.getStringFromClient()
		uprint(result)
		self.request.sendall(result.encode(self.encoding))
		return

# if __name__ == "__main__":
import socket
import threading

adress = ("localhost", 8090)
server = socketserver.TCPServer(adress, MyRequestHandler)
# ip, port = server.server_address

# server.serve_forever() 
# keeps running until Ctrl-Console

server.handle_request()