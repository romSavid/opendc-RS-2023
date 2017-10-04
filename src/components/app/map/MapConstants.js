export const MAP_SIZE = 50;
export const TILE_SIZE_IN_PIXELS = 100;
export const TILE_SIZE_IN_METERS = 0.5;
export const MAP_SIZE_IN_PIXELS = MAP_SIZE * TILE_SIZE_IN_PIXELS;

export const OBJECT_MARGIN_IN_PIXELS = TILE_SIZE_IN_PIXELS / 5;
export const TILE_PLUS_MARGIN_IN_PIXELS = TILE_SIZE_IN_PIXELS / 3;
export const OBJECT_SIZE_IN_PIXELS =
  TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2;

export const GRID_LINE_WIDTH_IN_PIXELS = 2;
export const WALL_WIDTH_IN_PIXELS = TILE_SIZE_IN_PIXELS / 8;
export const OBJECT_BORDER_WIDTH_IN_PIXELS = TILE_SIZE_IN_PIXELS / 12;
export const TILE_PLUS_WIDTH_IN_PIXELS = TILE_SIZE_IN_PIXELS / 10;

export const SIDEBAR_WIDTH = 350;
export const VIEWPORT_PADDING = 50;

export const RACK_FILL_ICON_WIDTH = OBJECT_SIZE_IN_PIXELS / 3;
export const RACK_FILL_ICON_OPACITY = 0.8;

export const MAP_MOVE_PIXELS_PER_EVENT = 20;
export const MAP_SCALE_PER_EVENT = 1.1;
export const MAP_MIN_SCALE = 0.5;
export const MAP_MAX_SCALE = 1.5;

export const MAX_NUM_UNITS_PER_MACHINE = 4;
export const DEFAULT_RACK_SLOT_CAPACITY = 42;
export const DEFAULT_RACK_POWER_CAPACITY = 10000;
