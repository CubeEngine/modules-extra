[craft-activator]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/craft-activator.jpg
# Usage

To enable pipes you need an activator:  
![Surround a diamond with hoppers][craft-activator]

Endpoints for the Pipes are:
 - Sticky Pistons to extract items.
 - Normal Pistons to insert items.

Point the piston into a chest or any other inventory block and then activate it by placing the special hopper onto the piston.

Connect the endpoints with glass and make sure to not include any loops.  
Rightclicking the piston with an empty hand will now show particles.  
If the setup is correct green particles will spawn otherwise the particles are yellow and red where the problem is.

### Filters

Both input and output endpoints allow filters.   To open the filter shift-rightclick on a activated endpoint and place the items you want to filter in it. Only exact matches will be transfered.

### Storage Units

To create a bigger storage Unit connect one endpoint to a Dropper pointing into quartz blocks.  
All chests connected to the quartz blocks will be filled up. (Again loops are not allowed)  
To extract items from this system point an observer block into any inventory block, activate it using a special hopper and connect it to the quartz blocks.
When opening and closing the observed inventory it will extract items from chests connected to all quartz blocks.
