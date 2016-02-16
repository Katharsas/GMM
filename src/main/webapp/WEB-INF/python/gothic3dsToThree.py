# start with
# blender --background --python <script.py> -- <script args>
# e.g.:
# blender --background --python ../../src/main/python/test.py

import sys, bpy, os, tempfile, socket, socketserver, threading

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

def blenderPluginsEnabled():
	# import
	try:
		bpy.ops.wm.addon_enable(module="KrxImpExp")
	except ImportError:
		print("Kerrax import/export plugin not found!")
		return False
	
	if hasattr(bpy.types, "IMPORT_SCENE_OT_krx3dsimp"):
		uprint("Kerrax 3DS export is available.")
	else:
		uprint("Kerrax 3DS export not found!")
		return False
	# export
	try:
		bpy.ops.wm.addon_enable(module="io_three")
	except ImportError:
		uprint("Threejs export plugin not found!")
		return False
		
	if hasattr(bpy.types, "EXPORT_OT_three"):
		uprint("Threejs export is available.")
	else:
		uprint("Threejs JSON export not found!")
		return False
	# all ok
	return True
	
	
class ConversionProtocolException(Exception):
    def __init__(self, received, *expected):
    	super().__init__("Protocol Violation! Expected one of '" + expected
						+ "' but was '" + received + "' !")

class ConversionRequestHandler(socketserver.BaseRequestHandler):
	
	# protocol:
	
	# conversion mode start
	CONVERSION_START = "CONVERSION_START"
	# conversion mode end
	CONVERSION_END = "CONVERSION_END"	
	# if in conversion mode, you will receive {
	CONVERSION_FROM = "FROM"
		# followed by 3DS path followed by
	CONVERSION_TO = "TO"
		# followed by JSON target path
	# } any number of times until conversion end
	
	# MAKE SURE YOU USE ENDSIGNAL EXACTLY TO SEPERATE STRINGS!
	# MAKE SURE YOU DECODE SPECIFIED ENCODING FORMAT!
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
			self.request.settimeout(0.1); # make sure recv doesnt get stuck
			try:
				self.byteBuffer += self.request.recv(512)
			except socket.timeout:
				pass
# 				uprint("Timed out on recv.")
			result = self.next()
		return result
	
	def assertProtocol(self, received, *eitherExpected):
# 		uprint("Conversion protocol message received: " + received)
		match = False
		for expected in eitherExpected:
			if received == expected:
				match = True
		if match == False:
			raise ConversionProtocolException(received, *eitherExpected)
		else:
			return
	
	def handle(self):
		"""
		Handles a complete client connection.
		After this method returns, client cannot send without new connection.
		"""
		self.assertProtocol(self.getStringFromClient(), self.CONVERSION_START)
		current = self.getStringFromClient()
		while current == self.CONVERSION_FROM:
			originalPath = self.getStringFromClient()
			self.assertProtocol(self.getStringFromClient(), self.CONVERSION_TO)
			targetPath = self.getStringFromClient()
			self.convertFiles(originalPath, targetPath)
			current = self.getStringFromClient()
		self.assertProtocol(current, self.CONVERSION_FROM, self.CONVERSION_END)
		self.request.sendall("Conversion success!".encode(self.encoding))
		return
	
	def convertFiles(selfs, original, target):
		uprint("Converting:")
		uprint(original)
		uprint(" -> " + target)
		# clear default scene
		bpy.ops.object.mode_set(mode='OBJECT')
		bpy.ops.object.select_all(action='SELECT')
		bpy.ops.object.delete()
		# import/export
		bpy.ops.import_scene.krx3dsimp(filepath = original, quiet = True)
		bpy.ops.export.three(filepath = target)

def main():
	if blenderPluginsEnabled() == False:
		uprint("Exiting script since plugins cannot be invoked!")
		sys.exit(0)
	
	adress = ("localhost", 8090)
	server = socketserver.TCPServer(adress, ConversionRequestHandler)
	# ip, port = server.server_address
	
	# server.serve_forever() 
	# keeps running until Ctrl-Console

	server.handle_request()
	server.server_close()
	
if __name__ == "__main__":
	main()
