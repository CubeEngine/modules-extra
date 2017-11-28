[craft-activator]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/craft-activator.jpg
[simple-setup]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/simple-setup.jpg
[endpoints]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/endpoints.jpg
[materials]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/materials.jpg
[filters]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/filters.jpg
[storage-unit-input]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/storage-unit-input.jpg
[storage-unit-output]: https://raw.githubusercontent.com/CubeEngine/modules-extra/master/itemduct/docs/image/storage-unit-output.jpg
# Usage

To enable pipes you need an activator:  
![Surround a diamond with hoppers][craft-activator]

Endpoints for the ItemDuct are:
 - Sticky Pistons to extract items.
 - Normal Pistons to insert items.
 - Dropper for storage unit insertion.
 - Observer for storage unit extraction.  
  ![EndPoints][endpoints]
 
ItemDuct Materials are:
 - Glass blocks and panes (connect to any glass)
 - Colored glass blocks and panes (connect to the same color)
 - Quartz blocks (for storage units)  
![Materials][materials]
___

### Simple Setup
![Simple Setup][simple-setup]  
 1. Point the pistons into a chest and then activate it by placing the activator onto the piston.  
 2. Connect the endpoints with glass and make sure to not include any loops.  
 3. A right click on the piston with an empty hand will now show particles.  
    If the setup is correct green particles will spawn otherwise the particles are yellow and red where the problem is.
 4. After closing the chest connected to a sticky piston ItemDuct will try to move as much items as possible to the other endpoints.   
    
### Filters

Both input and output endpoints allow filters.  
To open the filter shift-rightclick on an activated endpoint and place the items you want to filter in it.  
Only exact matches will be transferred.

Example filtering Dandelion and Poppy Flowers:  
![Filters][filters]

### Storage Units

To create a bigger storage Unit connect one endpoint to a Dropper pointing into quartz blocks.
All chests connected to the quartz blocks will be filled up. (Again loops are not allowed)  
![Storage Unit Input][storage-unit-input]
  
To extract items from this system point an observer block into any inventory block, activate it using a special hopper and connect it to the quartz blocks.  
When opening and closing the observed inventory it will extract items from chests connected to all quartz blocks.  
![Storage Unit Output][storage-unit-output]
