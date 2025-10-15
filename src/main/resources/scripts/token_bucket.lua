local key = KEYS[1]
local nowMillis = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRate = tonumber(ARGV[3])
local refillPeriodMillis = tonumber(ARGV[4])
local expireInMillis = tonumber(ARGV[5])

local tokens, lastRefill

-- get current token count and last refill time
local value = redis.call('GET', KEYS[1])

-- if not exists set initial token count and last refill time
if not value then
  tokens = capacity; lastRefill = nowMillis
else
  local parts = {} for part in string.gmatch(value, '[^:]+') do table.insert(parts, part) end
  tokens = tonumber(parts[1])
  lastRefill = tonumber(parts[2])

  -- calculate refill interval count
  local intervals = math.floor((nowMillis - lastRefill) / refillPeriodMillis)
  if intervals > 0 then
    -- calculate token count and last refill time
    tokens = math.min(capacity, tokens + (intervals * refillRate))
    lastRefill = lastRefill + (intervals * refillPeriodMillis)
  end
end

if tokens > 0 then
  tokens = tokens - 1
  redis.call('SET', KEYS[1], tokens .. ':' .. lastRefill, 'PX', expireInMillis)
  return 1
else
  redis.call('SET', KEYS[1], tokens .. ':' .. lastRefill, 'PX', expireInMillis)
  return 0
end