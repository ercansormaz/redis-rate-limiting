local value = redis.call('GET', KEYS[1])
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local leakRate = tonumber(ARGV[3])
local leakPeriod = tonumber(ARGV[4])
local water, lastLeak
if not value then
  water = 0; lastLeak = now
else
  local parts = {} for part in string.gmatch(value, '[^:]+') do table.insert(parts, part) end
  water = tonumber(parts[1])
  lastLeak = tonumber(parts[2])
  local intervals = math.floor((now - lastLeak) / leakPeriod)
  if intervals > 0 then
    water = math.max(0, water - (intervals * leakRate))
    lastLeak = lastLeak + (intervals * leakPeriod)
  end
end
local expireDuration = tonumber(ARGV[5])
if water < capacity then
  water = water + 1
  redis.call('SET', KEYS[1], water .. ':' .. lastLeak, 'PX', expireDuration)
  return 1
else
  redis.call('SET', KEYS[1], water .. ':' .. lastLeak, 'PX', expireDuration)
  return 0
end