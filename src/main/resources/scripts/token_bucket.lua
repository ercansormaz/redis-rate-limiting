local value = redis.call('GET', KEYS[1])
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRate = tonumber(ARGV[3])
local refillPeriod = tonumber(ARGV[4])
local tokens, lastRefill
if not value then
  tokens = capacity; lastRefill = now
else
  local parts = {} for part in string.gmatch(value, '[^:]+') do table.insert(parts, part) end
  tokens = tonumber(parts[1])
  lastRefill = tonumber(parts[2])
  local intervals = math.floor((now - lastRefill) / refillPeriod)
  if intervals > 0 then
    tokens = math.min(capacity, tokens + (intervals * refillRate))
    lastRefill = lastRefill + (intervals * refillPeriod)
  end
end
if tokens > 0 then
  tokens = tokens - 1
  redis.call('SET', KEYS[1], tokens .. ':' .. lastRefill, 'PX', ARGV[5])
  return 1
else
  redis.call('SET', KEYS[1], tokens .. ':' .. lastRefill, 'PX', ARGV[5])
  return 0
end