local key = KEYS[1]
local nowMillis = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local leakRate = tonumber(ARGV[3])
local leakPeriodMillis = tonumber(ARGV[4])
local expireInMillis = tonumber(ARGV[5])

local water, lastLeak

-- get current water level and last leak time
local value = redis.call('GET', key)

-- if not exists set initial water level and last leak time
if not value then
  water = 0; lastLeak = nowMillis
else
  local parts = {} for part in string.gmatch(value, '[^:]+') do table.insert(parts, part) end
  water = tonumber(parts[1])
  lastLeak = tonumber(parts[2])

  -- calculate leak interval count
  local intervals = math.floor((nowMillis - lastLeak) / leakPeriodMillis)
  if intervals > 0 then
    -- calculate new water level and last leak time
    water = math.max(0, water - (intervals * leakRate))
    lastLeak = lastLeak + (intervals * leakPeriodMillis)
  end
end

if water < capacity then
  water = water + 1
  redis.call('SET', key, water .. ':' .. lastLeak, 'PX', expireInMillis)
  return 1
else
  redis.call('SET', key, water .. ':' .. lastLeak, 'PX', expireInMillis)
  return 0
end