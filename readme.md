  Wendy Xu
  20631406 w85xu
  openjdk version "11.0.5" 2019-10-15
  macOS 10.14.5 (MacBook Pro 2017)
  Tested on Pixel C with API 29 Android 10.0 (Q)

All icons made by Pixel Perfect from www.flaticon.com
License: https://www.freepikcompany.com/legal#nav-flaticon-agreement

Notes:
	- Pencil/Highlighter/Eraser tools can be deselected by clicking on their respective 
	  icons again
	- Undo/Redo: Each PDF page has its own undo redo stack, so clicking undo/redo on a
	  page will only apply the changes from the current page
	- Undo/Redo: Behaves the same as undo/redo for Google docs, photoshop, etc. Undoing
	  an action then drawing again (with pen or highlighter) will clear the redo stack,
	  as in the action that was undone cannot be recovered using redo after drawing post
	  undo action
	- Erase: You may find that the collision detection is very accurate, please try more 
	  than once when clicking annotations/highlights when deleting if not erased on first 
	  try
	- Erase: You can only erase a line if the position you press down and release on is
	  the same (no "drag" tap)
	- Zoom/Pan: Zoom is implemented using buttons on left of the status bar found
	  at the bottom of the screen, does not work with finger motions. Zoom does not
	  rescale any drawings, only enlarges/shrinks the original pdf page. 
	  Pan was not implemented.
	- Multi-page PDFs: You can navigate through the pages using the left and right
	  arrow buttons on the left of the status bar found at the bottom of the screen
	- On exit: Current page and selected tool are also saved with drawing and highlighting
