#http://blender.stackexchange.com/questions/17817/how-do-i-batch-import-one-format-then-export-to-another
#
# use print(dir(<anything>)) to print a list of all attributes of <anything>
#


# Three.js
# Test if addon is enabled:
hasattr(bpy.types, "EXPORT_OT_three")

# Enable addon if not enabled already:
try:
	bpy.ops.wm.addon_enable(module="io_three")
except ImportError:
	print("Threejs export addon not found")
	
#Kerrax
try:
	bpy.ops.wm.addon_enable(module="KrxImpExp")
except ImportError:
	print("Kerrax import/export addon not found")

# Folgende Objekte sollten vorhanden & nutzbar sein:
#	EXPORT_SCENE_OT_krx3dsexp
#	EXPORT_SCENE_OT_krxascexp
	
#	IMPORT_SCENE_OT_krx3dsimp
#	IMPORT_SCENE_OT_krxascimp

#delete all
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete()

# blender file paths are realtive to where .exe is
#import with kerrax
bpy.ops.import_scene.krx3dsimp(filepath="test/OW_Focusplattform.3DS", quiet=True)

#export with three
bpy.ops.export.three(filepath="test/OW_Focusplattform.js")

# additional export options can be  found in io_three/__init__.py -> class ExportThree
# defaults can be found in constants.py -> EXPORT_OPTIONS



# blender --background --python test/test.py -- <scriptargs>
#  = start blender GUI-less, run script at ... with args ...

#http://blender.stackexchange.com/questions/6817/how-to-pass-command-line-arguments-to-a-blender-python-script